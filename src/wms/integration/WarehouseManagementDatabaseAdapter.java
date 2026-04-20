package wms.integration;

import com.jackfruit.scm.database.adapter.WarehouseManagementAdapter;
import com.jackfruit.scm.database.facade.SupplyChainDatabaseFacade;
import com.jackfruit.scm.database.model.Warehouse;
import com.jackfruit.scm.database.model.WarehouseModels;

import wms.models.GRN;
import wms.models.GRNItem;
import wms.models.Order;
import wms.models.Product;
import wms.models.WarehouseTask;
import wms.services.WMSLogger;
import wms.views.WarehouseTerminalView;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Integrates WMS with Subsystem 15 (Database Design) via the DB team's
 * WarehouseManagementAdapter.
 *
 * DESIGN PATTERN — Adapter (Object Adapter):
 * Translates WMS domain objects (GRN, Order, WarehouseTask, Product) into
 * the DB module's model classes (WarehouseModels records).
 * This is the ONLY file in WMS that imports com.jackfruit.scm classes.
 *
 * SOLID — Single Responsibility: handles only WMS-to-DB translation.
 * SOLID — Dependency Inversion: WMS services depend on this class through
 * the wiring in Main.java, never importing DB classes directly.
 *
 * GRASP — Indirection: sits between WMS services and the DB module,
 * keeping both sides decoupled.
 *
 * LIFECYCLE: SupplyChainDatabaseFacade implements AutoCloseable.
 * This adapter opens it at construction and closes it on shutdown.
 * The facade auto-recreates the OOAD schema on every instantiation —
 * DatabaseSeeder must run AFTER this adapter is constructed.
 *
 * EXCEPTION POLICY: logs via WMSLogger, takes no corrective action.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 15 (Database Design):
 * WMS accesses the database exclusively through this class.
 * No other WMS file imports com.jackfruit.scm classes directly.
 */
public class WarehouseManagementDatabaseAdapter {

    private SupplyChainDatabaseFacade facade;
    private WarehouseManagementAdapter warehouseAdapter;
    private boolean isOnline = false;

    // Fixed warehouse and zone IDs used throughout WMS operations.
    // These match the values seeded by DatabaseSeeder.
    public static final String DEFAULT_WAREHOUSE_ID = "WH-001";
    public static final String DEFAULT_ZONE_DRY     = "ZONE-DRY";
    public static final String DEFAULT_ZONE_COLD    = "ZONE-COLD";
    public static final String DEFAULT_ZONE_HIGH    = "ZONE-HIGH";

    /**
     * Initialises the DB facade and registers the default warehouse structure.
     *
     * The SupplyChainDatabaseFacade constructor auto-recreates the OOAD schema.
     * DatabaseSeeder must be called AFTER this constructor completes so that
     * the fresh schema is ready before Excel data is inserted.
     */
    public WarehouseManagementDatabaseAdapter() {
        try {
            facade = new SupplyChainDatabaseFacade();
            warehouseAdapter = new WarehouseManagementAdapter(facade);
            isOnline = true;

            WarehouseTerminalView.printSystemEvent("DB-ADAPTER",
                    "Subsystem 15 (Database) connected via WarehouseManagementAdapter.");

            // Register the default warehouse and zones so all subsequent
            // operations have a valid parent structure.
            registerDefaultWarehouseStructure();

        } catch (Throwable t) {
            isOnline = false;
            WMSLogger.logError("WarehouseManagementDatabaseAdapter.<init>",
                    "DB module unavailable: " + t.getMessage());
            WarehouseTerminalView.printWarning("DB-ADAPTER",
                    "Subsystem 15 OFFLINE — operating without DB persistence.");
        }
    }

    // ── WAREHOUSE STRUCTURE SETUP ─────────────────────────────────────────────

    /**
     * Registers the default WMS warehouse and its three zones (DRY, COLD, HIGH_VALUE).
     * Called once at startup. Idempotent — DB module handles duplicate gracefully.
     */
    private void registerDefaultWarehouseStructure() {
        try {
            // Register warehouse
            Warehouse warehouse = new Warehouse(DEFAULT_WAREHOUSE_ID, "Central Warehouse");
            warehouseAdapter.registerWarehouse(warehouse);

            // Register zones — types must match schema enum exactly:
            // STORAGE, PICKING, STAGING, RECEIVING, DISPATCH
            warehouseAdapter.createZone(new WarehouseModels.WarehouseZone(
                    DEFAULT_ZONE_DRY, DEFAULT_WAREHOUSE_ID, "STORAGE"));

            warehouseAdapter.createZone(new WarehouseModels.WarehouseZone(
                    DEFAULT_ZONE_COLD, DEFAULT_WAREHOUSE_ID, "STORAGE"));

            warehouseAdapter.createZone(new WarehouseModels.WarehouseZone(
                    DEFAULT_ZONE_HIGH, DEFAULT_WAREHOUSE_ID, "STORAGE"));

            warehouseAdapter.createZone(new WarehouseModels.WarehouseZone(
                    "ZONE-STAGING", DEFAULT_WAREHOUSE_ID, "STAGING"));

            warehouseAdapter.createZone(new WarehouseModels.WarehouseZone(
                    "ZONE-RECEIVING", DEFAULT_WAREHOUSE_ID, "RECEIVING"));
            // Register standard bins
            registerBin("ZONE-DRY-BIN-99",  DEFAULT_ZONE_DRY,  1000, "AVAILABLE");
            registerBin("ZONE-COLD-BIN-01", DEFAULT_ZONE_COLD, 500,  "AVAILABLE");
            registerBin("ZONE-HIGH-BIN-01", DEFAULT_ZONE_HIGH, 200,  "AVAILABLE");
            registerBin("PACK-STATION-1",   DEFAULT_ZONE_DRY,  200,  "AVAILABLE");
            registerBin("STAGING-OUT",      DEFAULT_ZONE_DRY,  500,  "AVAILABLE");

            WarehouseTerminalView.printSystemEvent("DB-ADAPTER",
                    "Default warehouse structure registered: "
                            + DEFAULT_WAREHOUSE_ID + " with 3 zones and 5 bins.");

        } catch (Throwable t) {
            // Structure may already exist from a previous run — log and continue.
            WMSLogger.logError("WarehouseManagementDatabaseAdapter.registerDefaultWarehouseStructure",
                    t.getMessage());
        }
    }

    /**
     * Registers a single bin in the DB.
     */
    private void registerBin(String binId, String zoneId,
                              int capacity, String status) {
        try {
            warehouseAdapter.createBin(
                    new WarehouseModels.Bin(binId, zoneId, capacity, status));
        } catch (Throwable t) {
            // Bin may already exist — not an error condition.
            WMSLogger.logError("WarehouseManagementDatabaseAdapter.registerBin",
                    "Bin " + binId + ": " + t.getMessage());
        }
    }

    // ── INBOUND OPERATIONS ────────────────────────────────────────────────────

    /**
     * Records a completed Goods Receipt Note in the DB.
     * Called by InboundReceivingController after processArrivalWithQC completes.
     *
     * @param grn       The completed GRN from InboundReceivingController
     * @param poId      The Purchase Order this GRN is against
     * @param supplierId The supplier who delivered the goods
     */
    public void recordGoodsReceipt(GRN grn, String poId, String supplierId) {
        if (!isOnline) return;
        try {
            for (GRNItem item : grn.getItems().values()) {
                WarehouseModels.GoodsReceipt receipt = new WarehouseModels.GoodsReceipt(
                        grn.getGrnId(),
                        poId,
                        supplierId,
                        item.getProductId(),
                        item.getAcceptedQty() + item.getDamagedQty(), // orderedQty
                        item.getAcceptedQty(),                         // receivedQty
                        LocalDateTime.now(),
                        item.getDamagedQty() > 0 ? "DAMAGED" : "GOOD"
                );
                warehouseAdapter.createGoodsReceipt(receipt);
            }
            WarehouseTerminalView.printSystemEvent("DB-ADAPTER",
                    "GRN recorded in DB: " + grn.getGrnId());
        } catch (Throwable t) {
            WMSLogger.logError("WarehouseManagementDatabaseAdapter.recordGoodsReceipt",
                    t.getMessage());
        }
    }

    // ── STOCK OPERATIONS ──────────────────────────────────────────────────────

    /**
     * Records a stock level update in the DB after putaway or receiving.
     * Called by InventoryManager after addStock() completes.
     *
     * @param productId The SKU of the product
     * @param binId     The bin where the stock is located
     * @param quantity  The current quantity in that bin
     */
    public void recordStockLevel(String productId, String binId, int quantity) {
        if (!isOnline) return;
        try {
            WarehouseModels.StockRecord record = new WarehouseModels.StockRecord(
                    UUID.randomUUID().toString(),
                    productId,
                    binId,
                    quantity,
                    LocalDateTime.now()
            );
            warehouseAdapter.createStockRecord(record);
            WarehouseTerminalView.printSystemEvent("DB-ADAPTER",
                    "Stock level recorded: " + productId
                            + " | Bin: " + binId + " | Qty: " + quantity);
        } catch (Throwable t) {
            WMSLogger.logError("WarehouseManagementDatabaseAdapter.recordStockLevel",
                    t.getMessage());
        }
    }

    /**
     * Records a stock movement (putaway, pick, transfer) in the DB.
     * Supports the Double-Entry Stock Keeping requirement.
     *
     * @param movementType  "INBOUND", "OUTBOUND", or "TRANSFER"
     * @param fromBin       Source bin ID (use "SUPPLIER" for inbound)
     * @param toBin         Destination bin ID (use "DISPATCH" for outbound)
     * @param productId     The SKU being moved
     * @param quantity      Number of units moved
     */
    public void recordStockMovement(String movementType, String fromBin,
                                     String toBin, String productId, int quantity) {
        if (!isOnline) return;
        try {
            WarehouseModels.StockMovement movement = new WarehouseModels.StockMovement(
                    UUID.randomUUID().toString(),
                    movementType,
                    fromBin,
                    toBin,
                    productId,
                    quantity,
                    LocalDateTime.now()
            );
            warehouseAdapter.createStockMovement(movement);
            WarehouseTerminalView.printSystemEvent("DB-ADAPTER",
                    "Stock movement recorded: " + movementType
                            + " | " + fromBin + " → " + toBin
                            + " | SKU: " + productId + " | Qty: " + quantity);
        } catch (Throwable t) {
            WMSLogger.logError("WarehouseManagementDatabaseAdapter.recordStockMovement",
                    t.getMessage());
        }
    }

    // ── OUTBOUND OPERATIONS ───────────────────────────────────────────────────

    /**
     * Records a pick task in the DB when OutboundTaskController completes a task.
     * Maps WMS WarehouseTask to the DB module's PickTask record.
     *
     * @param task    The completed WMS warehouse task
     * @param orderId The outbound order this task belongs to
     */
    public void recordPickTask(WarehouseTask task, String orderId) {
        if (!isOnline) return;
        try {
            WarehouseModels.PickTask pickTask = new WarehouseModels.PickTask(
                    task.getTaskId(),
                    orderId,
                    "SYSTEM",              // assignedEmployeeId — real impl maps worker ID
                    task.getProductId(),
                    1,                     // pickQty — real impl reads from task
                    task.getStatus()
            );
            warehouseAdapter.createPickTask(pickTask);
            WarehouseTerminalView.printSystemEvent("DB-ADAPTER",
                    "Pick task recorded in DB: " + task.getTaskId()
                            + " | Status: " + task.getStatus());
        } catch (Throwable t) {
            WMSLogger.logError("WarehouseManagementDatabaseAdapter.recordPickTask",
                    t.getMessage());
        }
    }

    /**
     * Records a staging dispatch event in the DB when goods are ready for shipment.
     * Called after a packing job is confirmed by Subsystem 14.
     *
     * @param orderId    The outbound order being dispatched
     * @param dockDoorId The dock door used for dispatch
     */
    public void recordStagingDispatch(String orderId, String dockDoorId) {
        if (!isOnline) return;
        try {
            WarehouseModels.StagingDispatch dispatch = new WarehouseModels.StagingDispatch(
                    UUID.randomUUID().toString(),
                    dockDoorId,
                    orderId,
                    LocalDateTime.now(),
                    "DISPATCHED"
            );
            warehouseAdapter.createStagingDispatch(dispatch);
            WarehouseTerminalView.printSystemEvent("DB-ADAPTER",
                    "Staging dispatch recorded: Order " + orderId
                            + " | Dock: " + dockDoorId);
        } catch (Throwable t) {
            WMSLogger.logError("WarehouseManagementDatabaseAdapter.recordStagingDispatch",
                    t.getMessage());
        }
    }

    // ── HEALTH AND LIFECYCLE ──────────────────────────────────────────────────

    /**
     * Returns whether the DB module is currently available.
     * Used by Main.java for startup health reporting.
     */
    public boolean isOnline() {
        return isOnline;
    }

    /**
     * Closes the SupplyChainDatabaseFacade connection pool.
     * Call this on application shutdown.
     */
    public void close() {
        if (facade != null) {
            try {
                facade.close();
                WarehouseTerminalView.printSystemEvent("DB-ADAPTER",
                        "Subsystem 15 DB connection closed.");
            } catch (Throwable t) {
                WMSLogger.logError("WarehouseManagementDatabaseAdapter.close",
                        t.getMessage());
            }
        }
    }
}
