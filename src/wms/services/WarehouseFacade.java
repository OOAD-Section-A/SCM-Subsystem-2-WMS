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
}