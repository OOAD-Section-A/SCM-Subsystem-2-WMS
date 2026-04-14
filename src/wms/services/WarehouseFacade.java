package wms.services;

import wms.contracts.IWMSRepository;
import wms.contracts.WarehouseSubsystemBase;
import wms.exceptions.WMSException;

/**
 * Description: Structural Pattern (Facade) applied here. 
 * This is the central controller for Subsystem 2.
 */
public class WarehouseFacade extends WarehouseSubsystemBase {

    // Dependency Injection of our new manager
    private InventoryManager inventoryManager;

    public WarehouseFacade(IWMSRepository repository) {
        super(repository);
        this.inventoryManager = new InventoryManager();
    }

    /**
     * Hook for Subsystem 4 (Order Fulfillment).
     */
    @Override
    public boolean reserveStockForOrder(String sku, int quantity) {
        System.out.println("Facade: Subsystem 4 (Order Fulfillment) requesting " + quantity + " units of " + sku);
        try {
            return inventoryManager.reserveStock(sku, quantity);
        } catch (WMSException e) {
            // We do not cancel the order ourselves; we just log it for Subsystem 17
            WMSLogger.logError("WarehouseFacade.reserveStockForOrder", e.getMessage());
            return false;
        }
    }

    @Override
    public void processInboundScan(String barcode, String dockId) {
        try {
            System.out.println("Facade: Processing inbound scan at " + dockId + " for barcode: " + barcode);
            if (barcode == null || barcode.isEmpty() || barcode.equals("INVALID")) {
                throw new WMSException("Invalid barcode scanned. Cannot process inbound.");
            }
            System.out.println("Facade: Scan processed successfully.");
        } catch (WMSException e) {
            WMSLogger.logError("WarehouseFacade.processInboundScan", e.getMessage());
        }
    }

    /**
     * Integrates the Strategy Pattern. Decides which algorithm to use based on product type.
     * UPDATED to include quantity and update the InventoryManager.
     */
    public void receiveAndStoreProduct(wms.models.Product product, int quantity) {
        System.out.println("Facade: Receiving product - " + product.getName() + " (Qty: " + quantity + ")");
        
        wms.strategies.IPutawayStrategy putawayStrategy;

        if (product.getCategory() == wms.models.ProductCategory.PERISHABLE_COLD) {
            putawayStrategy = new wms.strategies.ColdChainStrategy();
        } else {
            putawayStrategy = new wms.strategies.StandardFIFOStrategy();
        }

        String assignedBin = putawayStrategy.determineStorageBin(product);
        System.out.println("Facade: Product '" + product.getName() + "' successfully stored in " + assignedBin);
        
        // UPDATE INVENTORY LEDGER
        inventoryManager.addStock(product.getSku(), quantity);
    }

    /**
     * Integrates the Factory Pattern. Packs a product into a specific storage unit.
     */
    public wms.models.StorageUnit packProduct(wms.models.Product product, wms.models.StorageUnitType unitType, String unitId) {
        System.out.println("Facade: Request received to pack '" + product.getName() + "' into a " + unitType);
        wms.models.StorageUnit container = wms.factories.StorageUnitFactory.createStorageUnit(unitType, unitId);
        System.out.println("Facade: Packed into " + container.getClass().getSimpleName() + 
                           " | Tracking Method: " + container.getTrackingMethod() +
                           " | Max Capacity: " + container.getMaxWeightCapacityKg() + "kg");
        return container;
    }
}