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

        // --- 4. Upgraded Procurement: ASN & Pre-Receiving Test ---
        System.out.println("\n--- 4. Upgraded Procurement: ASN & Pre-Receiving Test ---");
        wms.models.Supplier dairyFarm = new wms.models.Supplier("SUP-001", "Green Valley Farms", 5, 0.95);
        wms.models.PurchaseOrder po = new wms.models.PurchaseOrder("PO-10023", dairyFarm);
        po.addExpectedItem(milk.getSku(), 50, 2.50); 
        System.out.println("Purchase Order " + po.getPoNumber() + " successfully created.");

        // Step A: Supplier sends ASN
        wms.models.AdvanceShipmentNotice asn = new wms.models.AdvanceShipmentNotice("ASN-7788", po.getPoNumber(), dairyFarm, "2026-04-20");
        asn.addExpectedItem(milk.getSku(), 50);

        // Step B: Warehouse registers the ASN
        wms.controllers.InboundReceivingController dockController = new wms.controllers.InboundReceivingController(wmsFacade);
        dockController.registerASN(asn);

        // Step C: Truck arrives and we process the receipt
        dockController.processArrival(po, asn, milk, 50);
        
        System.out.println("\n--- Test Complete ---");
    }
}