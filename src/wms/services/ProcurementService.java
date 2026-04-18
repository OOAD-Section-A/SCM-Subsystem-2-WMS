package wms.services;

import wms.models.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: Financial orchestrator handling the 3-Way Match logic.
 */
public class ProcurementService {

    /**
     * Compares Purchase Order (Ordered), GRN (Received), and Invoice (Billed).
     */
    public boolean execute3WayMatch(PurchaseOrder po, GRN grn, SupplierInvoice invoice) {
        System.out.println("\nProcurementService: Executing 3-Way Match for PO: " + po.getPoNumber());
        boolean isMatch = true;
        List<Discrepancy> discrepancies = new ArrayList<>();

        for (InvoiceItem invItem : invoice.getItems().values()) {
            String sku = invItem.getProductId();
            POItem poItem = po.getItem(sku);
            GRNItem grnItem = grn.getItem(sku);

            if (poItem == null || grnItem == null) {
                isMatch = false;
                continue;
            }

            // 1. Quantity Check (Short Supply & Over-billing)
            if (invItem.getBilledQty() > grnItem.getAcceptedQty()) {
                discrepancies.add(new Discrepancy("QUANTITY", sku, po.getSupplier().getSupplierId(),
                    "Short Supply / Over-billed. Accepted: " + grnItem.getAcceptedQty() + ", Billed: " + invItem.getBilledQty()));
                isMatch = false;
            }

            // 2. Price Check
            if (invItem.getBilledPrice() != poItem.getAgreedPrice()) {
                discrepancies.add(new Discrepancy("PRICE", sku, po.getSupplier().getSupplierId(),
                    "Price Mismatch. Agreed: $" + poItem.getAgreedPrice() + ", Billed: $" + invItem.getBilledPrice()));
                isMatch = false;
            }

            // 3. Damage Check
            if (grnItem.getDamagedQty() > 0) {
                discrepancies.add(new Discrepancy("DAMAGE", sku, po.getSupplier().getSupplierId(),
                    "Damaged Goods reported: " + grnItem.getDamagedQty() + " units. Invoice must be adjusted."));
                isMatch = false; 
            }
        }

        if (isMatch) {
            System.out.println("ProcurementService: 3-Way Match SUCCESS. Invoice " + invoice.getInvoiceId() + " authorized for payment.");
        } else {
            System.out.println("ProcurementService: 3-Way Match FAILED. Payment hold applied.");
            for (Discrepancy d : discrepancies) {
                d.printDetails();
            }
        }
        return isMatch;
    }
}