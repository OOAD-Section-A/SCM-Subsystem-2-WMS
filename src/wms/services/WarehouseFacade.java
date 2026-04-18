package wms.services;

import java.util.Map;
import wms.contracts.IWMSRepository;
import wms.contracts.WarehouseSubsystemBase;
import wms.exceptions.WMSException;

public class WarehouseFacade extends WarehouseSubsystemBase {

    private InventoryManager inventoryManager;
    private OrderPickingEngine pickingEngine;
    private wms.services.TaskEngine taskEngine;

    public WarehouseFacade(IWMSRepository repository) {
        super(repository);
        this.inventoryManager = new InventoryManager();
        this.pickingEngine = new OrderPickingEngine();
        this.taskEngine = new wms.services.TaskEngine();
    }

    public wms.services.TaskEngine getTaskEngine() {
        return this.taskEngine;
    }
    
    /**
     * Bridges digital reservation with physical execution.
     * Uses Strategy pattern for dynamic picking algorithms.
     */
    public boolean dispatchOrder(wms.models.Order order, wms.strategies.IPickingStrategy strategy) {
        System.out.println("\nFacade: Attempting to dispatch Order " + order.getOrderId());
        try {
            // 1. Digital Reservation (Fail Fast if stock is missing)
            for (Map.Entry<String, Integer> item : order.getLineItems().entrySet()) {
                inventoryManager.reserveStock(item.getKey(), item.getValue());
            }

            // 2. Physical Execution
            pickingEngine.setPickingStrategy(strategy);
            pickingEngine.executePicking(order);
            
            System.out.println("Facade: Order " + order.getOrderId() + " successfully dispatched to floor.");
            return true;

        } catch (WMSException e) {
            WMSLogger.logError("WarehouseFacade.dispatchOrder", "Failed to dispatch Order " + order.getOrderId() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean reserveStockForOrder(String sku, int quantity) {
        // Kept for simple backwards compatibility with basic Subsystem 4 calls
        try { return inventoryManager.reserveStock(sku, quantity); } 
        catch (WMSException e) { return false; }
    }

    @Override
    public void processInboundScan(String barcode, String dockId) {}

    public void receiveAndStoreProduct(wms.models.Product product, int quantity) {
        System.out.println("Facade: Receiving product - " + product.getName() + " (Qty: " + quantity + ")");
        wms.strategies.IPutawayStrategy putawayStrategy = (product.getCategory() == wms.models.ProductCategory.PERISHABLE_COLD) ? new wms.strategies.ColdChainStrategy() : new wms.strategies.StandardFIFOStrategy();
        System.out.println("Facade: Product successfully stored in " + putawayStrategy.determineStorageBin(product));
        inventoryManager.addStock(product.getSku(), quantity);
    }

    public wms.models.StorageUnit packProduct(wms.models.Product product, wms.models.StorageUnitType unitType, String unitId) {
        return wms.factories.StorageUnitFactory.createStorageUnit(unitType, unitId);
    }
    /**
     * Cross-Docking feature
     * Bypasses the Putaway logic and routes inbound goods directly to the shipping dock.
     */
    public void processCrossDock(wms.models.Product product, int quantity, String outboundOrderId) {
        System.out.println("\nFacade: [CROSS-DOCKING INITIATED] for " + product.getName() + " (Qty: " + quantity + ")");
        System.out.println(" -> Bypassing Putaway Strategies...");
        System.out.println(" -> Routing directly from Receiving Dock to Shipping Dock for Order: " + outboundOrderId);
        // Note: In Cross-docking, it never enters the main inventory ledger.
    }

    public InventoryManager getInventoryManager() {
        return this.inventoryManager;
    }
}