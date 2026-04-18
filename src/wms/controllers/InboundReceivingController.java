package wms.controllers;

import java.util.HashMap;
import java.util.Map;
import wms.exceptions.WMSException;
import wms.models.AdvanceShipmentNotice;
import wms.models.Product;
import wms.models.PurchaseOrder;
import wms.services.WMSLogger;
import wms.services.WarehouseFacade;

/**
 * Description: GRASP Controller. Handles dock-door pre-receiving (ASN) 
 * and receiving processes, handing off to the WMS Facade.
 */
public class InboundReceivingController {
    
    private WarehouseFacade wmsFacade;
    // Registry of incoming shipments (Pre-Receiving)
    private Map<String, AdvanceShipmentNotice> expectedShipments;

    public InboundReceivingController(WarehouseFacade wmsFacade) {
        this.wmsFacade = wmsFacade;
        this.expectedShipments = new HashMap<>();
    }

    /**
     * Step 2 of Inbound Flow: Supplier sends ASN.
     */
    public void registerASN(AdvanceShipmentNotice asn) {
        expectedShipments.put(asn.getAsnId(), asn);
        System.out.println("Controller: [PRE-RECEIVING] ASN " + asn.getAsnId() + " registered for PO: " + asn.getPoId());
        System.out.println(" -> Warehouse preparing dock for arrival on " + asn.getExpectedArrivalDate());
    }

    /**
     * Step 4 of Inbound Flow: Goods Arrive.
     */
    public void processArrival(PurchaseOrder po, AdvanceShipmentNotice asn, Product product, int quantity) {
        System.out.println("\nController: [RECEIVING] Truck arrived with ASN " + asn.getAsnId() + ". Validating " + quantity + "x '" + product.getName() + "'.");

        try {
            // 1. Check if we actually registered this ASN
            if (!expectedShipments.containsKey(asn.getAsnId())) {
                throw new WMSException("Unregistered ASN: " + asn.getAsnId() + ". Truck denied entry.");
            }

            // 2. Validate against the Purchase Order (Information Expert)
            boolean isAuthorized = po.authorizeReceiving(product.getSku(), quantity);
            
            if (!isAuthorized) {
                throw new WMSException("PO Validation Failed for SKU: " + product.getSku() + ". Unordered item or quantity exceeded.");
            }

            System.out.println("Controller: PO Validation successful. Handing off to Warehouse Facade for Putaway.");
            
            // 3. Handoff to the WMS
            wmsFacade.receiveAndStoreProduct(product, quantity);

        } catch (WMSException e) {
            WMSLogger.logError("InboundReceivingController.processArrival", e.getMessage());
        }
    }
}