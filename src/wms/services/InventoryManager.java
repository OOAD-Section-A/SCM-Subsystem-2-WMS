package wms.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import wms.exceptions.WMSException;
import wms.observers.IInventoryObserver;

/**
 * Description: GRASP Information Expert for stock levels.
 * Now acts as the Subject in the Observer Pattern.
 */
public class InventoryManager {
    private Map<String, Integer> stockLedger;
    private Map<String, Integer> safetyStockThresholds;
    
    // Observer Pattern: List of listeners
    private List<IInventoryObserver> observers;

    public InventoryManager() {
        this.stockLedger = new HashMap<>();
        this.safetyStockThresholds = new HashMap<>();
        this.observers = new ArrayList<>();
    }

    public void addObserver(IInventoryObserver observer) {
        observers.add(observer);
    }

    public void setSafetyStockThreshold(String sku, int threshold) {
        safetyStockThresholds.put(sku, threshold);
    }

    public void addStock(String sku, int quantity) {
        stockLedger.put(sku, stockLedger.getOrDefault(sku, 0) + quantity);
        System.out.println("InventoryManager: Added " + quantity + " units of " + sku + ". Total Available: " + stockLedger.get(sku));
    }

    public boolean reserveStock(String sku, int quantity) throws WMSException {
        int currentStock = stockLedger.getOrDefault(sku, 0);
        
        if (currentStock >= quantity) {
            int newStock = currentStock - quantity;
            stockLedger.put(sku, newStock);
            System.out.println("InventoryManager: Reserved " + quantity + " units of " + sku + ". Remaining: " + newStock);
            
            // Trigger Observers if stock falls below threshold
            int threshold = safetyStockThresholds.getOrDefault(sku, 0);
            if (newStock < threshold) {
                notifyObservers(sku, newStock, threshold);
            }
            return true;
        } else {
            throw new WMSException("Insufficient stock for SKU: " + sku + ". Requested: " + quantity + ", Available: " + currentStock);
        }
    }

    private void notifyObservers(String sku, int currentStock, int threshold) {
        for (IInventoryObserver observer : observers) {
            observer.onStockBelowThreshold(sku, currentStock, threshold);
        }
    }
}