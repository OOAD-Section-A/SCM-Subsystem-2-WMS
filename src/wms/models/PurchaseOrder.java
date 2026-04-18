package wms.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: GRASP Information Expert.
 */

public class PurchaseOrder {
    private String poId;
    private Supplier supplier;
    private String status;
    private Map<String, POItem> items;

    public PurchaseOrder(String poId, Supplier supplier) {
        this.poId = poId;
        this.supplier = supplier;
        this.status = "OPEN";
        this.items = new HashMap<>();
    }

    // Upgraded to include price, mapping to POItem table
    public void addExpectedItem(String sku, int quantity, double price) {
        items.put(sku, new POItem(this.poId, sku, quantity, price));
    }

    public String getPoNumber() { return poId; }
    public String getStatus() { return status; }
    
    public POItem getItem(String sku) {
        return items.get(sku);
    }

    /**
     * Retained for backward compatibility with our current controller.
     * Will be upgraded in Phase 3 during 3-Way Matching.
     */
    public boolean authorizeReceiving(String sku, int quantity) {
        if (!items.containsKey(sku)) {
            return false; 
        }
        POItem item = items.get(sku);
        if (item.getReceivedQty() + quantity > item.getOrderedQty()) {
            return false; 
        }
        item.addReceivedQty(quantity);
        
        // Auto-close PO if fully received
        checkAndClosePO();
        return true;
    }

    private void checkAndClosePO() {
        boolean allReceived = true;
        for (POItem item : items.values()) {
            if (item.getPendingQty() > 0) {
                allReceived = false;
                break;
            }
        }
        if (allReceived) {
            this.status = "CLOSED";
            System.out.println("PurchaseOrder: PO " + poId + " is now fully received and CLOSED.");
        }
    }
}