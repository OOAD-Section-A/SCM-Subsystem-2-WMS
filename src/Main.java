import wms.contracts.IWMSRepository;
import wms.services.WarehouseFacade;
import wms.models.*;
import wms.commands.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting SCM Subsystem 2 (Command Pattern Test) ---");

        IWMSRepository mockDbRepo = new IWMSRepository() {
            public boolean validatePurchaseOrder(String po) { return true; }
            public void recordStockMovement(String sku, String from, String to, int qty) {}
            public boolean isBinAvailable(String binId) { return true; }
        };

        WarehouseFacade wmsFacade = new WarehouseFacade(mockDbRepo);
        Product cannedBeans = new Product("SKU-CANNED-99", "Baked Beans", ProductCategory.DRY_GOODS);
        
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

        System.out.println("\n--- Test Complete ---");
    }
}