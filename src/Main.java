import wms.contracts.IWMSRepository;
import wms.services.WarehouseFacade;
import wms.services.VendorSelectionEngine;
import wms.models.*;
import wms.commands.*;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting SCM Subsystem 2 (Phase 5: Vendor Selection Test) ---");

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
        wmsFacade.getTaskEngine().scheduleTask(new CycleCountTask(cannedBeans.getSku(), "ZONE-DRY-BIN-99", wmsFacade.getInventoryManager()));
        wmsFacade.getTaskEngine().scheduleTask(new InterleavedTask("Worker-JohnDoe", "Aisle 4, Rack B", "Aisle 4, Rack A"));

        System.out.println("\n--- 3. Executing the Task Queue ---");
        wmsFacade.getTaskEngine().executeAllPendingTasks();

        System.out.println("\n--- 4. Upgraded Procurement: 3-Way Match ---");
        wms.models.Supplier dairyFarm = new wms.models.Supplier("SUP-001", "Green Valley Farms", 5, 0.95);
        wms.models.PurchaseOrder po = new wms.models.PurchaseOrder("PO-10023", dairyFarm);
        po.addExpectedItem(milk.getSku(), 50, 2.50); 
        wms.models.AdvanceShipmentNotice asn = new wms.models.AdvanceShipmentNotice("ASN-7788", po.getPoNumber(), dairyFarm, "2026-04-20");
        asn.addExpectedItem(milk.getSku(), 50);
        
        wms.controllers.InboundReceivingController dockController = new wms.controllers.InboundReceivingController(wmsFacade);
        dockController.registerASN(asn);
        wms.models.GRN generatedGrn = dockController.processArrivalWithQC(po, asn, milk, 50, 5);

        wms.models.SupplierInvoice badInvoice = new wms.models.SupplierInvoice("INV-99221", po.getPoNumber());
        badInvoice.addItem(milk.getSku(), 50, 3.00); 
        new wms.services.ProcurementService().execute3WayMatch(po, generatedGrn, badInvoice);

        // --- 5. Vendor Selection Test ---
        System.out.println("\n--- 5. Vendor Selection & Replenishment ---");
        System.out.println("Inventory Alert: Milk stock is low! Initiating Vendor Selection...");

        Supplier vendorA = new Supplier("SUP-001", "Green Valley Farms", 5, 0.95);
        Supplier vendorB = new Supplier("SUP-002", "National Dairy Corp", 2, 0.80);
        Supplier vendorC = new Supplier("SUP-003", "Local Artisan Milks", 8, 0.99);

        // Populate historical metrics (Quality 1-100, Delivery 1-100, Price 1-100, Service 1-100)
        VendorSelectionEngine.SupplierMetrics metricsA = new VendorSelectionEngine.SupplierMetrics(vendorA, 90, 85, 80, 95); // Good all-rounder
        VendorSelectionEngine.SupplierMetrics metricsB = new VendorSelectionEngine.SupplierMetrics(vendorB, 75, 95, 90, 70); // Fast but lower quality
        VendorSelectionEngine.SupplierMetrics metricsC = new VendorSelectionEngine.SupplierMetrics(vendorC, 98, 60, 70, 90); // High quality, slow delivery

        VendorSelectionEngine selectionEngine = new VendorSelectionEngine();
        Supplier winningVendor = selectionEngine.selectBestVendor(Arrays.asList(metricsA, metricsB, metricsC));
        
        System.out.println("Action: Auto-generating Replenishment Purchase Order with " + winningVendor.getName());
        
        System.out.println("\n--- Project Execution Complete ---");
    }
}