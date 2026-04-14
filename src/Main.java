import wms.contracts.IWMSRepository;
import wms.services.WarehouseFacade;
import wms.models.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting SCM Subsystem 2 Test ---");

        IWMSRepository mockDbRepo = new IWMSRepository() {
            public boolean validatePurchaseOrder(String po) { return true; }
            public void recordStockMovement(String sku, String from, String to, int qty) {}
            public boolean isBinAvailable(String binId) { return true; }
        };

        WarehouseFacade wmsFacade = new WarehouseFacade(mockDbRepo);
        Product milk = new Product("SKU-DAIRY-1", "Organic Milk", ProductCategory.PERISHABLE_COLD);
        Product cereal = new Product("SKU-DRY-1", "Corn Flakes", ProductCategory.DRY_GOODS);

        // 1. Stock the warehouse first!
        System.out.println("--- 1. Inbound Restocking ---");
        wmsFacade.receiveAndStoreProduct(milk, 50);
        wmsFacade.receiveAndStoreProduct(cereal, 50);

        System.out.println("\n--- 2. Order Picking Test (Subsystem 4 Integration) ---");
        
        // Setup Order 1 (Supermarket A needs items quickly)
        Order order1 = new Order("ORD-9001");
        order1.addItem(milk.getSku(), 10);
        
        // Execute with Wave Picking
        wmsFacade.dispatchOrder(order1, new wms.strategies.WavePickingStrategy());

        // Setup Order 2 (Supermarket B needs a large mixed order)
        Order order2 = new Order("ORD-9002");
        order2.addItem(milk.getSku(), 5);
        order2.addItem(cereal.getSku(), 20);
        
        // Execute with Zone Picking
        wmsFacade.dispatchOrder(order2, new wms.strategies.ZonePickingStrategy());
        
        // Setup Order 3 (Fails due to lack of stock!)
        Order order3 = new Order("ORD-ERROR");
        order3.addItem(cereal.getSku(), 100);
        wmsFacade.dispatchOrder(order3, new wms.strategies.WavePickingStrategy());

        System.out.println("\n--- Test Complete ---");
    }
}