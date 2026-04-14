package wms.services;

import java.util.HashMap;
import java.util.Map;
import wms.exceptions.WMSException;

/**
 * Description: GRASP Information Expert for stock levels.
 * Tracks what is physically available in the warehouse bins.
 */
public class InventoryManager {
    // Maps SKU -> Available Quantity
    private Map<String, Integer> stockLedger;

    public InventoryManager() {
        this.stockLedger = new HashMap<>();
    }

    /**
     * Called when inbound goods are successfully put away.
     */
    public void addStock(String sku, int quantity) {
        stockLedger.put(sku, stockLedger.getOrDefault(sku, 0) + quantity);
        System.out.println("InventoryManager: Added " + quantity + " units of " + sku + ". Total Available: " + stockLedger.get(sku));
    }

    /**
     * Called by Subsystem 4 (Order Fulfillment) to reserve items for shipping.
     */
    public boolean reserveStock(String sku, int quantity) throws WMSException {
        int currentStock = stockLedger.getOrDefault(sku, 0);
        
        if (currentStock >= quantity) {
            stockLedger.put(sku, currentStock - quantity);
            System.out.println("InventoryManager: Reserved " + quantity + " units of " + sku + ". Remaining: " + stockLedger.get(sku));
            return true;
        } else {
            // Throw exception if Subsystem 4 asks for more than we have
            throw new WMSException("Insufficient stock for SKU: " + sku + ". Requested: " + quantity + ", Available: " + currentStock);
        }
    }
}
