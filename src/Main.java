import wms.contracts.IWMSRepository;
import wms.services.WarehouseFacade;
import wms.models.*;
import wms.observers.ReplenishmentService;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting SCM Subsystem 2 (Advanced Features Test) ---");

        IWMSRepository mockDbRepo = new IWMSRepository() {
            public boolean validatePurchaseOrder(String po) { return true; }
            public void recordStockMovement(String sku, String from, String to, int qty) {}
            public boolean isBinAvailable(String binId) { return true; }
        };

        WarehouseFacade wmsFacade = new WarehouseFacade(mockDbRepo);
        
        // 1. Setup Observer Pattern for Replenishment
        ReplenishmentService replService = new ReplenishmentService();
        wmsFacade.getInventoryManager().addObserver(replService);
        
        Product milk = new Product("SKU-DAIRY-1", "Organic Milk", ProductCategory.PERISHABLE_COLD);
        
        // Set the threshold. If milk drops below 20, trigger an alert!
        wmsFacade.getInventoryManager().setSafetyStockThreshold(milk.getSku(), 20);

        System.out.println("\n--- 1. Testing Automated Replenishment ---");
        wmsFacade.receiveAndStoreProduct(milk, 50); // Stock is now 50
        
        // Fulfill an order of 40. Stock will drop to 10. This should trigger the Observer!
        Order bigOrder = new Order("ORD-9999");
        bigOrder.addItem(milk.getSku(), 40);
        wmsFacade.dispatchOrder(bigOrder, new wms.strategies.WavePickingStrategy());

        System.out.println("\n--- 2. Testing Cross-Docking ---");
        Product freshBread = new Product("SKU-BREAD-1", "Fresh Bakery Bread", ProductCategory.PERISHABLE_COLD);
        // Bread is highly perishable and needed for an urgent order. Bypass storage!
        wmsFacade.processCrossDock(freshBread, 100, "ORD-URGENT-01");

        System.out.println("\n--- Test Complete ---");
    }
}