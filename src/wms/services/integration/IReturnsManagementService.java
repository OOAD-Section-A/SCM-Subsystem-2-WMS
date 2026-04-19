package wms.services.integration;

/**
 * Handles items returned from Delivery Agents.
 */
public interface IReturnsManagementService {
    /**
     * Processes a reverse logistics scan.
     * @param sku The returned item.
     * @param deliveryOrderId The original delivery.
     * @param reason The reason for return.
     */
    void processReturnScan(String sku, String deliveryOrderId, String reason);
}
