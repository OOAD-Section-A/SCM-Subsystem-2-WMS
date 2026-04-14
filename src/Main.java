import wms.contracts.IWMSRepository;
import wms.services.WarehouseFacade;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting SCM Subsystem 2 (Warehouse Management) Test ---");

        // 1. Mocking Subsystem 15 (Database Design)
        IWMSRepository mockDbRepo = new IWMSRepository() {
            public boolean validatePurchaseOrder(String po) { return true; }
            public void recordStockMovement(String sku, String from, String to, int qty) {}
            public boolean isBinAvailable(String binId) { return true; }
        };

        // 2. Initializing our Facade
        WarehouseFacade wmsFacade = new WarehouseFacade(mockDbRepo);
        
        System.out.println(wmsFacade.getSubsystemStatus());

        // 3. Testing a successful operation
        wmsFacade.processInboundScan("SKU-10045", "Dock-A");

        // 4. Testing Exception Handling (Subsystem 17 integration)
        wmsFacade.processInboundScan("INVALID", "Dock-B");
        
        System.out.println("--- Test Complete ---");
    }
}
