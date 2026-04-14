import wms.contracts.IWMSRepository;
import wms.services.WarehouseFacade;
import wms.models.Product;
import wms.models.ProductCategory;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting WMS Putaway Strategy Test ---");

        // Mock Subsystem 15
        IWMSRepository mockDbRepo = new IWMSRepository() {
            public boolean validatePurchaseOrder(String po) { return true; }
            public void recordStockMovement(String sku, String from, String to, int qty) {}
            public boolean isBinAvailable(String binId) { return true; }
        };

        WarehouseFacade wmsFacade = new WarehouseFacade(mockDbRepo);

        // Create some sample supermarket products
        Product milk = new Product("SKU-DAIRY-1", "Organic Whole Milk", ProductCategory.PERISHABLE_COLD);
        Product cereal = new Product("SKU-DRY-1", "Corn Flakes", ProductCategory.DRY_GOODS);

        // Test the dynamic strategy routing
        wmsFacade.receiveAndStoreProduct(milk);
        System.out.println("--------------------------------------------------");
        wmsFacade.receiveAndStoreProduct(cereal);

        System.out.println("--- Test Complete ---");
    }
}
