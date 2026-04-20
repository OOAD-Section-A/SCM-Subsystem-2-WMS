package wms.integration;

import wms.models.Order;
import wms.models.Product;
import wms.exceptions.WMSException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the Strategy Pattern for outbound packing verification.
 * Uses GRASP Information Expert by owning verification logic where order line
 * item expectations and scanned SKU inputs are available.
 * Connects Subsystem 11 (Barcode Reader and RFID Tracker) scan results to
 * Subsystem 14 (Packaging, Repairs, Receipt Management) dispatch.
 * Follows project exception handling policy by logging and surfacing
 * exceptions without taking corrective action in this class.
 */
public class PackingVerificationService implements IPackingVerificationService {

    @Override
    public boolean verifyAndDispatch(Order order, List<String> scannedSkus, Map<String, Product> productCatalog,
            IWarehousePackingIntegration packagingAdapter) throws WMSException {
        System.out
                .println("PackingVerification: Validating RFID scans for Outbound Order " + order.getOrderId() + "...");

        Map<String, Integer> scannedCounts = new HashMap<>();
        // Count each scanned SKU occurrence from the outbound RFID scan feed.
        for (String sku : scannedSkus) {
            scannedCounts.put(sku, scannedCounts.getOrDefault(sku, 0) + 1);
        }

        Map<String, Integer> expectedItems = order.getLineItems();

        // Verify every expected order item exists in scans with exact quantity.
        for (Map.Entry<String, Integer> entry : expectedItems.entrySet()) {
            String expectedSku = entry.getKey();
            int expectedQty = entry.getValue();
            int scannedQty = scannedCounts.getOrDefault(expectedSku, 0);

            if (scannedQty != expectedQty) {
                throw new WMSException("Packing mismatch for " + expectedSku + "! Expected: " + expectedQty
                        + ", Scanned: " + scannedQty);
            }
        }

        // Reject any scanned SKU that is not part of the order definition.
        for (String scannedSku : scannedCounts.keySet()) {
            if (!expectedItems.containsKey(scannedSku)) {
                throw new WMSException(
                        "Unauthorized item packed! SKU " + scannedSku + " is not in Order " + order.getOrderId());
            }
        }

        System.out.println("PackingVerification: RFID Validation PASSED. Handing off to External Packaging System...");

        // Translate the verified WMS order into a packing job request for Subsystem 14.
        // The order ID serves as the task identifier, and STAGING-OUT represents the
        // outbound staging bin where picked goods are physically waiting to be packed.
        packagingAdapter.createPackingJob(order.getOrderId(), "STAGING-OUT");

        return true;
    }
}
