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
import wms.integration.YardManagementService;
import wms.observers.ReplenishmentService;
import wms.services.SlottingOptimizerService;
import wms.services.WarehouseFacade;
import wms.views.WarehouseTerminalView;

public class Main {
    public static void main(String[] args) {

        // ── BOOT BANNER ──────────────────────────────────────────────────────
        WarehouseTerminalView.printSystemEvent("BOOT", "============================================================");
        WarehouseTerminalView.printSystemEvent("BOOT", "Initializing SCM Warehouse Management Subsystem (WMS)...");
        WarehouseTerminalView.printSystemEvent("BOOT", "Composition Root: wiring adapters, services, and controllers");
        WarehouseTerminalView.printSystemEvent("BOOT", "============================================================");

        // ── STEP 1: DATABASE SEEDING ─────────────────────────────────────────
        // Reads procurement_final_dataset.xlsx via Apache POI and hydrates MySQL.
        // Must run before any repository call to ensure FK constraints are satisfied.
        DatabaseSeeder.seedInitialData();

        // ── STEP 2: REPOSITORY LAYER (Circuit Breaker / Resilient Proxy) ─────
        // Primary: SCMDatabaseAdapter (MySQL via JDBC)
        // Fallback: InMemoryWMSRepository (in-memory cache)
        // ResilientRepositoryProxy transparently switches to fallback on failure.
        // PATTERN: Proxy + Circuit Breaker
        SCMDatabaseAdapter scmDatabaseAdapter = new SCMDatabaseAdapter();
        IWMSRepository fallbackRepo = new InMemoryWMSRepository();
        IWMSRepository activeRepository = new ResilientRepositoryProxy(scmDatabaseAdapter, fallbackRepo);

        // ── STEP 3: SUBSYSTEM 11 — RFID ADAPTER ──────────────────────────────
        // IRFIDSystemAdapter is the Anti-Corruption Layer between WMS and
        // Subsystem 11 (Barcode Reader and RFID Tracker).
        // Currently runs as a self-contained stub. When Subsystem 11 provides
        // their JAR, only RFIDSystemAdapter.java needs updating.
        // PATTERN: Adapter | SOLID: Dependency Inversion
        IRFIDSystemAdapter rfidAdapter = new RFIDSystemAdapter();

        // ── STEP 4: WAREHOUSE FACADE ──────────────────────────────────────────
        // WarehouseFacade receives null for the contracts repository because all
        // repository operations are handled by OutboundTaskController independently.
        // The facade itself never reads from the repository directly.
        WarehouseFacade wmsFacade = new WarehouseFacade(null, rfidAdapter);

        // ── STEP 5: OBSERVER — REPLENISHMENT SERVICE ──────────────────────────
        // Registers ReplenishmentService as an observer on InventoryManager.
        // Triggers automatic replenishment alerts when stock drops below threshold.
        // PATTERN: Observer | GRASP: Low Coupling
        ReplenishmentService replenishmentService = new ReplenishmentService();
        wmsFacade.getInventoryManager().addObserver(replenishmentService);
        wmsFacade.getInventoryManager().setSafetyStockThreshold("SKU-DAIRY-1", 100);
        wmsFacade.getInventoryManager().setSafetyStockThreshold("SKU-BREAD-3", 50);
        WarehouseTerminalView.printSystemEvent("BOOT", "Replenishment observer registered for SKU-DAIRY-1, SKU-BREAD-3.");

        // ── STEP 6: SUBSYSTEM 14 — PACKING ADAPTER ───────────────────────────
        // Connects WMS to Subsystem 14 (Packaging, Repairs, Receipt Management).
        // Constructor auto-detects JAR availability and falls back gracefully.
        // PATTERN: Adapter + Proxy | SOLID: Open/Closed
        Subsystem14PackingAdapter packingAdapter = new Subsystem14PackingAdapter();
        WarehouseTerminalView.printSystemEvent("BOOT",
                "Packing integration adapter ready: " + packingAdapter.getClass().getSimpleName());

        // ── STEP 7: SUBSYSTEM 8 — DEMAND FORECASTING + SLOTTING ──────────────
        // Subsystem8ForecastAdapter connects to Subsystem 8 (Demand Forecasting).
        // SlottingOptimizerService uses forecast data to optimize bin assignments.
        Subsystem8ForecastAdapter forecastAdapter = new Subsystem8ForecastAdapter();
        SlottingOptimizerService optimizer = new SlottingOptimizerService(forecastAdapter);
        optimizer.evaluateAndOptimizeSlotting("P-50", "BULK-ZONE-9", "FAST-PICK-1");

        // ── STEP 8: YARD AND CROSS-DOCKING SERVICES ───────────────────────────
        // YardManagementService: manages dock door assignments and geofence arrivals.
        // CrossDockingService: bypasses putaway for urgent backorder items.
        IYardManagementService yardService = new YardManagementService();
        ICrossDockingService crossDockService = new CrossDockingService();
        WarehouseTerminalView.printSystemEvent("BOOT", "Yard and Cross-Docking services initialised.");

        // ── STEP 9: INBOUND RECEIVING CONTROLLER ─────────────────────────────
        // GRASP Controller for the full inbound dock-to-shelf flow.
        // Depends on WarehouseFacade (which already holds the RFID adapter).
        InboundReceivingController inboundController = new InboundReceivingController(wmsFacade);
        WarehouseTerminalView.printSystemEvent("BOOT", "InboundReceivingController ready.");

        // ── STEP 10: OUTBOUND TASK POLLER (Async Background Thread) ──────────
        // Polls pending tasks from the repository on a background thread.
        // Uses Circuit Breaker, retry logic, and DLQ after 3 failures.
        // Sacred Architecture — do not modify the polling loop structure.
        WarehouseTerminalView.printSystemEvent("BOOT", "Starting asynchronous Outbound Task Poller...");
        OutboundTaskController outboundTaskController = new OutboundTaskController(activeRepository);
        Thread pollerThread = new Thread(outboundTaskController);
        pollerThread.setName("WMS-OutboundPoller");
        pollerThread.start();

        // ── BOOT COMPLETE ─────────────────────────────────────────────────────
        WarehouseTerminalView.printSystemEvent("BOOT",
                "WMS Subsystem fully initialized and awaiting integration triggers.");
        WarehouseTerminalView.printSystemEvent("BOOT", "============================================================");
    }
}