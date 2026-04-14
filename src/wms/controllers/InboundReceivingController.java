package wms.controllers;

import wms.exceptions.WMSException;
import wms.models.Product;
import wms.models.PurchaseOrder;
import wms.services.WMSLogger;
import wms.services.WarehouseFacade;

/**
 * Description: GRASP Controller. Handles the dock-door receiving process, 
 * validates against Procurement (POs), and hands off to the WMS Facade.
 */
public class InboundReceivingController {
    
    private WarehouseFacade wmsFacade;

    public InboundReceivingController(WarehouseFacade wmsFacade) {
        this.wmsFacade = wmsFacade;
    }

    /**
     * Simulates a dock worker scanning an item off a truck.
     */
    public void processArrival(PurchaseOrder po, Product product, int quantity) {
        System.out.println("Controller: Truck arrived. Validating " + quantity + "x '" + product.getName() + "' against PO: " + po.getPoNumber());

        try {
            // Validate with the Information Expert (PurchaseOrder)
            boolean isAuthorized = po.authorizeReceiving(product.getSku(), quantity);
            
            if (!isAuthorized) {
                // If invalid, throw to our subsystem 17 integration
                throw new WMSException("PO Validation Failed for SKU: " + product.getSku() + ". Unordered item or quantity exceeded.");
            }

            System.out.println("Controller: Validation successful. Handing off to Warehouse Facade for Putaway.");
            
            // If authorized, simulate storing each item (In reality, loop through quantity)
            wmsFacade.receiveAndStoreProduct(product, quantity);

        } catch (WMSException e) {
            // We do NOT store the product. We just log it.
            WMSLogger.logError("InboundReceivingController.processArrival", e.getMessage());
        }
    }
}