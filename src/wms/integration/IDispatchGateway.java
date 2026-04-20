package wms.services.integration;

import wms.exceptions.WMSException;
import java.util.List;

/**
 * Interface representing the gate scanner when a delivery truck departs.
 */
public interface IDispatchGateway {
    /**
     * Confirms the departure of a delivery agent's vehicle.
     * @param deliveryOrderId The external delivery order ID.
     * @param scannedSkus The bulk list of all SKUs scanned on the truck.
     * @throws WMSException if unauthorized packages are detected.
     */
    void processGatePass(String deliveryOrderId, List<String> scannedSkus) throws WMSException;
}
