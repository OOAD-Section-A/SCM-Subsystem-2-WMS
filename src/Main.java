import wms.contracts.IWMSRepository;
import wms.services.WarehouseFacade;
import wms.models.*;
import wms.commands.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting SCM Subsystem 2 (Procurement Models Upgrade Test) ---");

        IWMSRepository mockDbRepo = new IWMSRepository() {
            public boolean validatePurchaseOrder(String po) { return true; }
            public void recordStockMovement(String sku, String from, String to, int qty) {}
            public boolean isBinAvailable(String binId) { return true; }
        };

        WarehouseFacade wmsFacade = new WarehouseFacade(mockDbRepo);
        
        // Defining our products here so they can be used throughout the test
        Product cannedBeans = new Product("SKU-CANNED-99", "Baked Beans", ProductCategory.DRY_GOODS);
        Product milk = new Product("SKU-DAIRY-1", "Organic Milk", ProductCategory.PERISHABLE_COLD); // <-- FIX IS HERE

        System.out.println("\n--- 1. Stocking Inventory ---");
        wmsFacade.receiveAndStoreProduct(cannedBeans, 500);

        System.out.println("\n--- 2. Scheduling Advanced Worker Tasks ---");
        
        // Command 1: Schedule a Cycle Count (Audit)
        CycleCountTask auditTask = new CycleCountTask(
            cannedBeans.getSku(), 
            "ZONE-DRY-BIN-99", 
            wmsFacade.getInventoryManager()
        );
        wmsFacade.getTaskEngine().scheduleTask(auditTask);

        // Command 2: Schedule an Interleaved Task (Optimizing worker movement)
        InterleavedTask smartRoutingTask = new InterleavedTask(
            "Worker-JohnDoe", 
            "Aisle 4, Rack B",  // Drop off location
            "Aisle 4, Rack A"   // Pick up location on the way back
        );
        wmsFacade.getTaskEngine().scheduleTask(smartRoutingTask);

        System.out.println("\n--- 3. Executing the Task Queue ---");
        // The Invoker processes the commands
        wmsFacade.getTaskEngine().executeAllPendingTasks();

        // --- 4. Upgraded Procurement PO Test ---
        System.out.println("\n--- 4. Upgraded Procurement PO Test ---");
        wms.models.Supplier dairyFarm = new wms.models.Supplier("SUP-001", "Green Valley Farms", 5, 0.95);
        wms.models.PurchaseOrder po = new wms.models.PurchaseOrder("PO-10023", dairyFarm);
        
        // Now 'milk' exists and can be added to the PO!
        po.addExpectedItem(milk.getSku(), 50, 2.50); 
        
        System.out.println("Purchase Order " + po.getPoNumber() + " successfully created with Supplier: " + dairyFarm.getName());
        
        System.out.println("\n--- Test Complete ---");
    }
}