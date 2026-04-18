package wms.observers;

/**
 * Description: Behavioral Pattern (Observer).
 * Interface for any service that needs to listen to inventory changes.
 */
public interface IInventoryObserver {
    void onStockBelowThreshold(String sku, int currentStock, int threshold);
}