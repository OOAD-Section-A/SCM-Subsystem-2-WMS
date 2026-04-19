package wms.services.integration;

import wms.models.Order;
import wms.exceptions.WMSException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackingVerificationService implements IPackingVerificationService {

    @Override
    public boolean verifyPacking(Order order, List<String> scannedSkus) throws WMSException {
        System.out.println("PackingVerification: Validating RFID scans for Order " + order.getOrderId() + "...");
        
        Map<String, Integer> scannedCounts = new HashMap<>();
        for (String sku : scannedSkus) {
            scannedCounts.put(sku, scannedCounts.getOrDefault(sku, 0) + 1);
        }

        Map<String, Integer> expectedItems = order.getLineItems();
        
        // Check for missing or mismatched quantities
        for (Map.Entry<String, Integer> entry : expectedItems.entrySet()) {
            String expectedSku = entry.getKey();
            int expectedQty = entry.getValue();
            int scannedQty = scannedCounts.getOrDefault(expectedSku, 0);

            if (scannedQty != expectedQty) {
                throw new WMSException("Packing mismatch for " + expectedSku + "! Expected: " + expectedQty + ", Scanned: " + scannedQty);
            }
        }
        
        // Check for extra items that aren't even in the order
        for (String scannedSku : scannedCounts.keySet()) {
            if (!expectedItems.containsKey(scannedSku)) {
                throw new WMSException("Unauthorized item packed! SKU " + scannedSku + " is not in Order " + order.getOrderId());
            }
        }

        System.out.println("PackingVerification: SUCCESS! Order " + order.getOrderId() + " is accurately packed and ready for shipping.");
        return true;
    }
}
