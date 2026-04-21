import wms.controllers.InboundReceivingController;
import wms.controllers.OutboundTaskController;
import wms.integration.CrossDockingService;
import wms.integration.DatabaseSeeder;
import wms.integration.ICrossDockingService;
import wms.integration.IRFIDSystemAdapter;
import wms.integration.IWMSRepository;
import wms.integration.IYardManagementService;
import wms.integration.InMemoryWMSRepository;
import wms.integration.RFIDSystemAdapter;
import wms.integration.ResilientRepositoryProxy;
import wms.integration.SCMDatabaseAdapter;
import wms.integration.Subsystem14PackingAdapter;
import wms.integration.Subsystem8ForecastAdapter;
import wms.integration.WarehouseManagementDatabaseAdapter;
import wms.integration.YardManagementService;
import wms.observers.ReplenishmentService;
import wms.services.SlottingOptimizerService;
import wms.services.WarehouseFacade;
import wms.views.WarehouseTerminalView;
import wms.integration.TMSIntegrationService;

public class Main {
    public static void main(String[] args) {

        // ── BOOT BANNER ───────────────────────────────────────────────────────
        WarehouseTerminalView.printSystemEvent("BOOT", "============================================================");
        WarehouseTerminalView.printSystemEvent("BOOT", "Initializing SCM Warehouse Management Subsystem (WMS)...");
        WarehouseTerminalView.printSystemEvent("BOOT", "Composition Root: wiring adapters, services, and controllers");
        WarehouseTerminalView.printSystemEvent("BOOT", "============================================================");

        // ── STEP 1: SUBSYSTEM 14 — PACKING ADAPTER ───────────────────────────
        // Constructed FIRST because its DatabaseLayerFactory auto-detects the
        // shared DB JAR. Must complete before our DB adapter to avoid any
        // classpath-level interference during schema setup.
        // PATTERN: Adapter + Proxy | SOLID: Open/Closed
        Subsystem14PackingAdapter packingAdapter = new Subsystem14PackingAdapter();
        WarehouseTerminalView.printSystemEvent("BOOT",
                "Packing integration adapter ready: " + packingAdapter.getClass().getSimpleName());

        // ── STEP 2: SUBSYSTEM 15 — DB ADAPTER ────────────────────────────────
        // SupplyChainDatabaseFacade auto-recreates the OOAD schema on construction.
        // Must run AFTER Packing (Step 1) and BEFORE DatabaseSeeder (Step 3)
        // so the schema is fresh and stable when Excel data is inserted.
        // PATTERN: Adapter | SOLID: Dependency Inversion
        WarehouseManagementDatabaseAdapter dbAdapter = new WarehouseManagementDatabaseAdapter();
        WarehouseTerminalView.printSystemEvent("BOOT",
                "Subsystem 15 DB adapter status: "
                        + (dbAdapter.isOnline() ? "ONLINE" : "OFFLINE — degraded mode"));

        // ── STEP 3: DATABASE SEEDING ──────────────────────────────────────────
        // Reads procurement_final_dataset.xlsx via Apache POI and hydrates MySQL.
        // Runs on the stable schema created in Step 2.
        // Sacred Architecture — DatabaseSeeder uses Apache POI + FK-ordered inserts.
        DatabaseSeeder.seedInitialData();

        // ── STEP 4: REPOSITORY LAYER (Circuit Breaker / Resilient Proxy) ─────
        // Primary: SCMDatabaseAdapter (MySQL via JDBC)
        // Fallback: InMemoryWMSRepository (in-memory cache)
        // ResilientRepositoryProxy transparently switches to fallback on failure.
        // PATTERN: Proxy + Circuit Breaker
        SCMDatabaseAdapter scmDatabaseAdapter = new SCMDatabaseAdapter();
        IWMSRepository fallbackRepo = new InMemoryWMSRepository();
        IWMSRepository activeRepository = new ResilientRepositoryProxy(scmDatabaseAdapter, fallbackRepo);

        // ── STEP 5: SUBSYSTEM 11 — RFID ADAPTER ──────────────────────────────
        // Anti-Corruption Layer between WMS and Subsystem 11 (RFID Tracker).
        // Stub mode until Subsystem 11 fixes WMSIntegrationService constructor.
        // PATTERN: Adapter | SOLID: Dependency Inversion
        IRFIDSystemAdapter rfidAdapter = new RFIDSystemAdapter();

        // ── STEP 6: WAREHOUSE FACADE ──────────────────────────────────────────
        // Single entry point for all inbound and outbound warehouse operations.
        // Passes null for contracts repository — OutboundTaskController owns that.
        // PATTERN: Facade | GRASP: Controller
        WarehouseFacade wmsFacade = new WarehouseFacade(null, rfidAdapter);

        // ── STEP 7: OBSERVER — REPLENISHMENT SERVICE ──────────────────────────
        // Registers ReplenishmentService as an observer on InventoryManager.
        // Fires when stock drops below configured safety stock threshold.
        // PATTERN: Observer | GRASP: Low Coupling
        ReplenishmentService replenishmentService = new ReplenishmentService();
        wmsFacade.getInventoryManager().addObserver(replenishmentService);
        wmsFacade.getInventoryManager().setSafetyStockThreshold("SKU-DAIRY-1", 100);
        wmsFacade.getInventoryManager().setSafetyStockThreshold("SKU-BREAD-3", 50);
        WarehouseTerminalView.printSystemEvent("BOOT",
                "Replenishment observer registered for SKU-DAIRY-1, SKU-BREAD-3.");

        // ── STEP 8: SUBSYSTEM 8 — DEMAND FORECASTING + SLOTTING ──────────────
        // Subsystem8ForecastAdapter connects to Subsystem 8 (Demand Forecasting).
        // SlottingOptimizerService uses forecast data to optimize bin assignments.
        Subsystem8ForecastAdapter forecastAdapter = new Subsystem8ForecastAdapter();
        SlottingOptimizerService optimizer = new SlottingOptimizerService(forecastAdapter);
        optimizer.evaluateAndOptimizeSlotting("P-50", "BULK-ZONE-9", "FAST-PICK-1");

        // ── STEP 9: YARD AND CROSS-DOCKING SERVICES ───────────────────────────
        // YardManagementService: dock door assignments and geofence arrivals.
        // CrossDockingService: bypasses putaway for urgent backorder items.
        IYardManagementService yardService = new YardManagementService();
        ICrossDockingService crossDockService = new CrossDockingService();
        WarehouseTerminalView.printSystemEvent("BOOT",
                "Yard and Cross-Docking services initialised.");

        // ── STEP 10: INBOUND RECEIVING CONTROLLER ─────────────────────────────
        // GRASP Controller for the full inbound dock-to-shelf flow.
        // Owns ASN registration, GRN creation, QC, and putaway strategy selection.
        InboundReceivingController inboundController = new InboundReceivingController(wmsFacade);
        WarehouseTerminalView.printSystemEvent("BOOT",
                "InboundReceivingController ready.");

        // ── STEP 10B: SUBSYSTEM 6 — TMS INTEGRATION SERVICE ───────────────────────
        // Singleton event store that TMS polls for shipment ready and rejection events.
        // WMS publishes events here; TMS consumes them independently.
        // Zero coupling — WMS never imports or references any TMS class.
        // PATTERN: Facade + Observer (polling) | GRASP: Low Coupling
        TMSIntegrationService tmsService = TMSIntegrationService.getInstance();
        WarehouseTerminalView.printSystemEvent("BOOT",
        "TMS Integration Service ready. " + tmsService.getQueueStatus());

        // ── STEP 11: OUTBOUND TASK POLLER (Async Background Thread) ──────────
        // Background Runnable polling pending tasks from the repository.
        // Circuit Breaker, 3-retry DLQ, SafeExceptionAdapter on failure.
        // Sacred Architecture — do not modify the polling loop structure.
        WarehouseTerminalView.printSystemEvent("BOOT",
                "Starting asynchronous Outbound Task Poller...");
        OutboundTaskController outboundTaskController =
                new OutboundTaskController(activeRepository);
        Thread pollerThread = new Thread(outboundTaskController);
        pollerThread.setName("WMS-OutboundPoller");
        pollerThread.start();

        // ── BOOT COMPLETE ──────────────────────────────────────────────────────
        WarehouseTerminalView.printSystemEvent("BOOT",
                "WMS Subsystem fully initialized and awaiting integration triggers.");
        WarehouseTerminalView.printSystemEvent("BOOT",
                "============================================================");
    }
}