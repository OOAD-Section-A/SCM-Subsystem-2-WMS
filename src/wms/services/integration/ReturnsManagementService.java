package wms.services.integration;

public class ReturnsManagementService implements IReturnsManagementService {

    @Override
    public void processReturnScan(String sku, String deliveryOrderId, String reason) {
        System.out.println("ReturnsManagement: Inbound reverse logistics scan detected.");
        System.out.println(" -> Item: " + sku + " | From Order: " + deliveryOrderId);
        System.out.println(" -> Reason: " + reason);
        System.out.println("ReturnsManagement: Routing " + sku + " to Reverse Logistics QC holding area.");
    }
}
