package wms.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: GRASP Information Expert. Holds the expected line items and 
 * validates if incoming goods match what was ordered.
 */
public class PurchaseOrder {
    private String poNumber;
    private Supplier supplier;
    // Maps SKU -> Expected Quantity
    private Map<String, Integer> expectedItems;
    // Maps SKU -> Actually Received Quantity
    private Map<String, Integer> receivedItems;

    public PurchaseOrder(String poNumber, Supplier supplier) {
        this.poNumber = poNumber;
        this.supplier = supplier;
        this.expectedItems = new HashMap<>();
        this.receivedItems = new HashMap<>();
    }

    public void addExpectedItem(String sku, int quantity) {
        expectedItems.put(sku, quantity);
        receivedItems.put(sku, 0); // Initialize received count to 0
    }

    public String getPoNumber() { return poNumber; }

    /**
     * Validates if we can receive this SKU.
     * Prevents over-receiving or accepting unordered items.
     */
    public boolean authorizeReceiving(String sku, int quantity) {
        if (!expectedItems.containsKey(sku)) {
            return false; // We didn't order this item
        }
        
        int currentlyReceived = receivedItems.get(sku);
        int expected = expectedItems.get(sku);
        
        if (currentlyReceived + quantity > expected) {
            return false; // Over-receiving attempt
        }
        
        // Update received counts
        receivedItems.put(sku, currentlyReceived + quantity);
        return true;
    }
}
