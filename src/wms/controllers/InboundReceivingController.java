package wms.controllers;

import java.util.HashMap;
import java.util.Map;
import wms.exceptions.WMSException;
import wms.models.*;
import wms.services.WMSLogger;
import wms.services.WarehouseFacade;

/**
 * Description: GRASP Controller. Handles dock-door pre-receiving, GRN creation, 
 * Quality Inspection (QC), and Damaged Goods rules.
 */
public class InboundReceivingController {
    
    private WarehouseFacade wmsFacade;
    private Map<String, AdvanceShipmentNotice> expectedShipments;

    public InboundReceivingController(WarehouseFacade wmsFacade) {
        this.wmsFacade = wmsFacade;
        this.expectedShipments = new HashMap<>();
    }

    public void registerASN(AdvanceShipmentNotice asn) {
        expectedShipments.put(asn.getAsnId(), asn);
        System.out.println("Controller: [PRE-RECEIVING] ASN " + asn.getAsnId() + " registered for PO: " + asn.getPoId());
    }

    /**
     * Upgraded: Now incorporates GRN and Quality Inspection logic.
     */
    public void processArrivalWithQC(PurchaseOrder po, AdvanceShipmentNotice asn, Product product, int receivedQty, int damagedQty) {
        System.out.println("\nController: [RECEIVING] Truck arrived with ASN " + asn.getAsnId() + ".");

        try {
            if (!expectedShipments.containsKey(asn.getAsnId())) {
                throw new WMSException("Unregistered ASN: " + asn.getAsnId() + ". Truck denied entry.");
            }

            // 1. Create GRN
            System.out.println("Controller: Generating Goods Receipt Note (GRN)...");
            GRN grn = new GRN("GRN-" + System.currentTimeMillis(), po.getPoNumber());
            grn.addItem(product.getSku(), receivedQty, damagedQty);
            GRNItem grnItem = grn.getItem(product.getSku());

            // 2. Perform Quality Inspection
            System.out.println("Controller: Performing Quality Inspection...");
            QualityInspection qir = new QualityInspection(
                "QIR-" + System.currentTimeMillis(), grn.getGrnId(), product.getSku(), 
                grnItem.getAcceptedQty(), grnItem.getDamagedQty(), "Routine Dock QC"
            );
            qir.printReport();

            // 3. Apply Damaged Goods Rule 
            if (grnItem.getDamagedQty() > 0) {
                if (product.getCategory() == ProductCategory.PERISHABLE_COLD) {
                    System.out.println("QC ALERT: " + grnItem.getDamagedQty() + "x Perishable items damaged. Action: DISPOSE.");
                } else {
                    System.out.println("QC ALERT: " + grnItem.getDamagedQty() + "x Non-perishable items damaged. Action: RTV (Return to Vendor).");
                }
            }

            if (grnItem.getAcceptedQty() <= 0) {
                throw new WMSException("Entire shipment failed QC. Shipment rejected.");
            }

            // 4. Update PO and Handoff ONLY the accepted goods
            boolean isAuthorized = po.authorizeReceiving(product.getSku(), grnItem.getAcceptedQty());
            if (!isAuthorized) {
                throw new WMSException("PO Validation Failed. Trying to receive more than ordered.");
            }

            System.out.println("Controller: QC & PO Validation successful. Handing off " + grnItem.getAcceptedQty() + " units to Warehouse Facade.");
            wmsFacade.receiveAndStoreProduct(product, grnItem.getAcceptedQty());

        } catch (WMSException e) {
            WMSLogger.logError("InboundReceivingController.processArrivalWithQC", e.getMessage());
        }
    }
}