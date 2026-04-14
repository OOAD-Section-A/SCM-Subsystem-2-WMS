import wms.contracts.IWMSRepository;
import wms.services.WarehouseFacade;
import wms.models.Product;
import wms.models.ProductCategory;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting SCM Subsystem 2 Test ---");

        IWMSRepository mockDbRepo = new IWMSRepository() {
            public boolean validatePurchaseOrder(String po) { return true; }
            public void recordStockMovement(String sku, String from, String to, int qty) {}
            public boolean isBinAvailable(String binId) { return true; }
        };

        WarehouseFacade wmsFacade = new WarehouseFacade(mockDbRepo);
        Product milk = new Product("SKU-DAIRY-1", "Organic Whole Milk", ProductCategory.PERISHABLE_COLD);

        System.out.println("--- 1. Inbound Receiving Test ---");
        wms.models.Supplier dairyFarm = new wms.models.Supplier("SUP-001", "Green Valley Farms");
        wms.models.PurchaseOrder po = new wms.models.PurchaseOrder("PO-10023", dairyFarm);
        po.addExpectedItem(milk.getSku(), 50);

        wms.controllers.InboundReceivingController dockController = new wms.controllers.InboundReceivingController(wmsFacade);
        
        // We receive 10 units. This should update our inventory!
        dockController.processArrival(po, milk, 10);
        
        System.out.println("\n--- 2. Subsystem 4 (Order Fulfillment) Integration Test ---");
        
        // Scenario A: Subsystem 4 asks for 5 milks (We have 10, so this should succeed)
        boolean order1Success = wmsFacade.reserveStockForOrder(milk.getSku(), 5);
        System.out.println("Order 1 Fulfillment Status: " + (order1Success ? "SUCCESS" : "FAILED"));

        System.out.println();

        // Scenario B: Subsystem 4 asks for 20 milks (We only have 5 left, so this should fail and log to Subsystem 17)
        boolean order2Success = wmsFacade.reserveStockForOrder(milk.getSku(), 20);
        System.out.println("Order 2 Fulfillment Status: " + (order2Success ? "SUCCESS" : "FAILED"));

        System.out.println("\n--- Test Complete ---");
    }
}