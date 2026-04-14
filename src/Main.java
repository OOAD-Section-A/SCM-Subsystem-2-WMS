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

        // Test the Factory Pattern
        System.out.println("--------------------------------------------------");
        System.out.println("--- Starting WMS Storage Unit Factory Test ---");
        wmsFacade.packProduct(milk, wms.models.StorageUnitType.PALLET, "PAL-9901");
        System.out.println();
        wmsFacade.packProduct(cereal, wms.models.StorageUnitType.CASE, "CAS-5520");

        // Test Feature 5: Inbound Receiving & Procurement
        System.out.println("--------------------------------------------------");
        System.out.println("--- Starting WMS Inbound Receiving Test ---");

        // 1. Setup Procurement Data
        wms.models.Supplier dairyFarm = new wms.models.Supplier("SUP-001", "Green Valley Farms");
        wms.models.PurchaseOrder po = new wms.models.PurchaseOrder("PO-10023", dairyFarm);
        
        // We ordered exactly 50 units of Milk
        po.addExpectedItem(milk.getSku(), 50);

        // 2. Setup the Controller
        wms.controllers.InboundReceivingController dockController = new wms.controllers.InboundReceivingController(wmsFacade);

        // 3. Test a valid receipt (Dock worker scans 10 units of milk)
        dockController.processArrival(po, milk, 10);
        System.out.println();

        // 4. Test an invalid receipt (Dock worker scans cereal, but we only ordered milk!)
        dockController.processArrival(po, cereal, 5);

        System.out.println("--- Test Complete ---");
    }
}