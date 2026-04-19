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
        wms.services.integration.IRFIDAuditorService auditorService = new wms.services.integration.RFIDAuditorService();
        java.util.List<String> simulatedBinTags = java.util.Collections.nCopies(500, "RFID-TAG-CANNED-BEANS");
        wmsFacade.getTaskEngine().scheduleTask(new CycleCountTask(cannedBeans.getSku(), "ZONE-DRY-BIN-99", wmsFacade.getInventoryManager(), auditorService, simulatedBinTags));
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

        System.out.println("\n--- Simulating Item-by-Item RFID Scans at Dock-A ---");
        for (int i = 0; i < 50; i++) {
            wmsFacade.processInboundScan(milk.getSku(), "Dock-A");
        }

        wms.models.GRN generatedGrn = dockController.processArrivalWithQC(po, asn, milk, "Dock-A", 5);

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

        // --- 6. Extended RFID Integrations ---
        System.out.println("\n--- 6. WMS/RFID Integration Services ---");
        
        System.out.println("\n[A] Outbound Packing Verification");
        wms.services.integration.IPackingVerificationService packingService = new wms.services.integration.PackingVerificationService();
        wms.models.Order outboundOrder = new wms.models.Order("ORD-5544");
        outboundOrder.addItem(milk.getSku(), 2);
        java.util.List<String> packedTags = java.util.Arrays.asList(milk.getSku(), milk.getSku());
        try {
            packingService.verifyPacking(outboundOrder, packedTags);
        } catch (wms.exceptions.WMSException e) {
            System.err.println(e.getMessage());
        }

        System.out.println("\n[B] Cross-Docking Automation");
        wms.services.integration.CrossDockingService crossDockService = new wms.services.integration.CrossDockingService();
        crossDockService.addUrgentBackorder("SKU-URGENT-55");
        wms.models.Product urgentProduct = new wms.models.Product("SKU-URGENT-55", "Emergency Meds", wms.models.ProductCategory.PERISHABLE_COLD);
        if (!crossDockService.evaluateCrossDocking(urgentProduct)) {
            wmsFacade.receiveAndStoreProduct(urgentProduct, 10);
        }

        System.out.println("\n[C] Dispatch & Gate Pass");
        wms.services.integration.IDispatchGateway dispatchGateway = new wms.services.integration.DispatchGateway();
        try {
            dispatchGateway.processGatePass("DEL-ORD-9988", java.util.Arrays.asList("TAG-1", "TAG-2", "TAG-3"));
        } catch (wms.exceptions.WMSException e) {
            System.err.println(e.getMessage());
        }

        System.out.println("\n[D] Returns Management");
        wms.services.integration.IReturnsManagementService returnsService = new wms.services.integration.ReturnsManagementService();
        returnsService.processReturnScan("SKU-CANNED-99", "DEL-ORD-1122", "Customer Refused Delivery");

        System.out.println("\n--- 7. Real-Time Delivery Integrations ---");
        
        System.out.println("\n[E] Yard Management (Geofence Arrival)");
        wms.services.integration.YardManagementService yardService = new wms.services.integration.YardManagementService();
        yardService.simulateOccupiedDock("Dock-B", "ASN-OLD-999"); // Force a double-booking exception
        try {
            yardService.handleGeofenceArrival("ASN-NEW-123", "TRUCK-XYZ");
        } catch (wms.exceptions.WMSException e) {
            System.err.println(e.getMessage());
        }

        System.out.println("\n[F] Cold Chain Handoff Verification");
        wms.services.integration.IColdChainVerificationService coldChainService = new wms.services.integration.ColdChainVerificationService();
        try {
            // Simulated RFID temp reading 5.2C (Breach > 4.0C), Transit alert = false
            coldChainService.verifyTemperatureHandoff(milk.getSku(), "Dock-A", 5.2, false);
        } catch (wms.exceptions.WMSException e) {
            System.err.println(e.getMessage());
        }
        
        System.out.println("\n--- Project Execution Complete ---");
    }
}