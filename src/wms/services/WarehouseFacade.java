package wms.services;

import wms.contracts.IWMSRepository;
import wms.contracts.WarehouseSubsystemBase;
import wms.exceptions.WMSException;

/**
 * Description: Structural Pattern (Facade) applied here. 
 * This is the central controller for Subsystem 2.
 */
public class WarehouseFacade extends WarehouseSubsystemBase {

    public WarehouseFacade(IWMSRepository repository) {
        super(repository);
    }

    @Override
    public boolean reserveStockForOrder(String sku, int quantity) {
        System.out.println("Facade: Checking stock for SKU: " + sku);
        // We will implement the actual logic later using Strategy pattern
        return true; 
    }

    @Override
    public void processInboundScan(String barcode, String dockId) {
        try {
            System.out.println("Facade: Processing inbound scan at " + dockId + " for barcode: " + barcode);
            
            // Simulating a business rule violation
            if (barcode == null || barcode.isEmpty() || barcode.equals("INVALID")) {
                throw new WMSException("Invalid barcode scanned. Cannot process inbound.");
            }
            
            // Logic for DB integration will go here
            System.out.println("Facade: Scan processed successfully.");

        } catch (WMSException e) {
            // GRASP / SOLID compliance: We do not fix the error, we pass it to Subsystem 17
            WMSLogger.logError("WarehouseFacade.processInboundScan", e.getMessage());
        }
    }
    /**
     * Integrates the Strategy Pattern. Decides which algorithm to use based on product type.
     */
    public void receiveAndStoreProduct(wms.models.Product product) {
        System.out.println("Facade: Receiving product - " + product.getName());
        
        wms.strategies.IPutawayStrategy putawayStrategy;

        // Context dynamically selects the strategy
        if (product.getCategory() == wms.models.ProductCategory.PERISHABLE_COLD) {
            putawayStrategy = new wms.strategies.ColdChainStrategy();
        } else {
            putawayStrategy = new wms.strategies.StandardFIFOStrategy();
        }

        String assignedBin = putawayStrategy.determineStorageBin(product);
        System.out.println("Facade: Product '" + product.getName() + "' successfully stored in " + assignedBin);
        
        // Note: Here we would call Subsystem 15's repository to save this to the DB, 
        // and notify Subsystem 12 (Double-entry Stock Keeping).
    }
    /**
     * Integrates the Factory Pattern. Packs a product into a specific storage unit.
     */
    public wms.models.StorageUnit packProduct(wms.models.Product product, wms.models.StorageUnitType unitType, String unitId) {
        System.out.println("Facade: Request received to pack '" + product.getName() + "' into a " + unitType);
        
        // Use the Factory to create the container (Creational Pattern)
        wms.models.StorageUnit container = wms.factories.StorageUnitFactory.createStorageUnit(unitType, unitId);
        
        System.out.println("Facade: Packed into " + container.getClass().getSimpleName() + 
                           " | Tracking Method: " + container.getTrackingMethod() +
                           " | Max Capacity: " + container.getMaxWeightCapacityKg() + "kg");
        
        return container;
    }
  }