import wms.contracts.IWMSRepository;
import wms.services.WarehouseFacade;
import wms.models.*;
import wms.commands.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting SCM Subsystem 2 (Phase 4: 3-Way Match Test) ---");

        IWMSRepository mockDbRepo = new IWMSRepository() {
            public boolean validatePurchaseOrder(String po) { return true; }
            public void recordStockMovement(String sku, String from, String to, int qty) {}
            public boolean isBinAvailable(String binId) { return true; }
        };

        WarehouseFacade wmsFacade = new WarehouseFacade(mockDbRepo);
        
        Product cannedBeans = new Product("SKU-CANNED-99", "Baked Beans", ProductCategory.DRY_GOODS);
        Product milk = new Product("SKU-DAIRY-1", "Organic Milk", ProductCategory.PERISHABLE_COLD); 

        System.out.println("\n--- 1. Stocking Inventory ---");
        wmsFacade.receiveAndStoreProduct(cannedBeans, 500);

        System.out.println("\n--- 2. Scheduling Advanced Worker Tasks ---");
        CycleCountTask auditTask = new CycleCountTask(cannedBeans.getSku(), "ZONE-DRY-BIN-99", wmsFacade.getInventoryManager());
        wmsFacade.getTaskEngine().scheduleTask(auditTask);

        InterleavedTask smartRoutingTask = new InterleavedTask("Worker-JohnDoe", "Aisle 4, Rack B", "Aisle 4, Rack A");
        wmsFacade.getTaskEngine().scheduleTask(smartRoutingTask);

        System.out.println("\n--- 3. Executing the Task Queue ---");
        wmsFacade.getTaskEngine().executeAllPendingTasks();

        // --- 4. Upgraded Procurement: 3-Way Match & Exceptions ---
        System.out.println("\n--- 4. Upgraded Procurement: 3-Way Match & Exceptions ---");
        wms.models.Supplier dairyFarm = new wms.models.Supplier("SUP-001", "Green Valley Farms", 5, 0.95);
        wms.models.PurchaseOrder po = new wms.models.PurchaseOrder("PO-10023", dairyFarm);
        
        // We order 50 milks at $2.50 each
        po.addExpectedItem(milk.getSku(), 50, 2.50); 

        wms.models.AdvanceShipmentNotice asn = new wms.models.AdvanceShipmentNotice("ASN-7788", po.getPoNumber(), dairyFarm, "2026-04-20");
        asn.addExpectedItem(milk.getSku(), 50);

        wms.controllers.InboundReceivingController dockController = new wms.controllers.InboundReceivingController(wmsFacade);
        dockController.registerASN(asn);

        // Truck arrives: 45 pass, 5 damaged
        wms.models.GRN generatedGrn = dockController.processArrivalWithQC(po, asn, milk, 50, 5);

        // Supplier sends us an invoice trying to charge for 50 milks at $3.00!
        wms.models.SupplierInvoice badInvoice = new wms.models.SupplierInvoice("INV-99221", po.getPoNumber());
        badInvoice.addItem(milk.getSku(), 50, 3.00); 

        // Execute 3-Way Match
        wms.services.ProcurementService procurementService = new wms.services.ProcurementService();
        procurementService.execute3WayMatch(po, generatedGrn, badInvoice);
        
        System.out.println("\n--- Test Complete ---");
    }
}