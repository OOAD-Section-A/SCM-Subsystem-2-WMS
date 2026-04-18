package wms.commands;

import wms.services.InventoryManager;

/**
 * Description: Concrete Command. Performs a partial inventory audit 
 * without shutting down the warehouse
 */
public class CycleCountTask implements IWarehouseTask {
    private String sku;
    private String binLocation;
    private InventoryManager inventoryManager;

    public CycleCountTask(String sku, String binLocation, InventoryManager inventoryManager) {
        this.sku = sku;
        this.binLocation = binLocation;
        this.inventoryManager = inventoryManager;
    }

    @Override
    public void execute() {
        System.out.println("\n[TASK EXECUTING] Cycle Count for SKU: " + sku + " at Bin: " + binLocation);
        System.out.println(" -> Worker scanning bin contents...");
        System.out.println(" -> System records verified against physical count. No discrepancies found.");
    }
}
