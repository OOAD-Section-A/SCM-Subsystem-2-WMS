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

    public GRN processArrivalWithQC(PurchaseOrder po, AdvanceShipmentNotice asn, Product product, int receivedQty, int damagedQty) {
        System.out.println("\nController: [RECEIVING] Truck arrived with ASN " + asn.getAsnId() + ".");
        try {
            if (!expectedShipments.containsKey(asn.getAsnId())) throw new WMSException("Unregistered ASN.");
            System.out.println("Controller: Generating Goods Receipt Note (GRN)...");
            GRN grn = new GRN("GRN-" + System.currentTimeMillis(), po.getPoNumber());
            grn.addItem(product.getSku(), receivedQty, damagedQty);
            GRNItem grnItem = grn.getItem(product.getSku());

            System.out.println("Controller: Performing Quality Inspection...");
            QualityInspection qir = new QualityInspection("QIR-" + System.currentTimeMillis(), grn.getGrnId(), product.getSku(), grnItem.getAcceptedQty(), grnItem.getDamagedQty(), "Routine Dock QC");
            qir.printReport();

            if (grnItem.getDamagedQty() > 0) {
                if (product.getCategory() == ProductCategory.PERISHABLE_COLD) System.out.println("QC ALERT: " + grnItem.getDamagedQty() + "x Perishable items damaged. Action: DISPOSE.");
                else System.out.println("QC ALERT: " + grnItem.getDamagedQty() + "x Non-perishable items damaged. Action: RTV (Return to Vendor).");
            }

            if (grnItem.getAcceptedQty() <= 0) throw new WMSException("Entire shipment failed QC. Shipment rejected.");
            boolean isAuthorized = po.authorizeReceiving(product.getSku(), grnItem.getAcceptedQty());
            if (!isAuthorized) throw new WMSException("PO Validation Failed.");

            System.out.println("Controller: QC & PO Validation successful. Handing off " + grnItem.getAcceptedQty() + " units to Warehouse Facade.");
            wmsFacade.receiveAndStoreProduct(product, grnItem.getAcceptedQty());
            
            return grn; // Return the GRN for financial matching!

        } catch (WMSException e) {
            WMSLogger.logError("InboundReceivingController.processArrivalWithQC", e.getMessage());
            return null;
        }
    }
}