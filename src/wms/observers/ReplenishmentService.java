package wms.observers;

/**
 * Description: Concrete Observer. Automatically triggers stock moves from 
 * bulk storage to active pick faces when inventory is low
 */
public class ReplenishmentService implements IInventoryObserver {
    @Override
    public void onStockBelowThreshold(String sku, int currentStock, int threshold) {
        System.out.println("⚠️ [REPLENISHMENT ALERT]: Stock for " + sku + " dropped to " + currentStock + " (Threshold: " + threshold + ").");
        System.out.println(" -> Generating automated task: Move 50 units from Bulk Storage to Active Pick Face.");
    }
}