package wms.services.integration;

import wms.exceptions.WMSException;
import java.util.List;

public class DispatchGateway implements IDispatchGateway {

    @Override
    public void processGatePass(String deliveryOrderId, List<String> scannedSkus) throws WMSException {
        System.out.println("DispatchGateway: Scanning truck for Delivery Order: " + deliveryOrderId + " at Exit Gate...");
        
        if (scannedSkus.isEmpty()) {
            throw new WMSException("Gate Pass Failed! Truck is empty.");
        }

        System.out.println("DispatchGateway: " + scannedSkus.size() + " packages verified in truck.");
        System.out.println("DispatchGateway: Gate Pass Approved! Truck dispatched.");
        
        // This is the simulated trigger to Subsystem 6 (Realtime Monitoring)
        System.out.println("--> WMS NOTIFY: Firing Webhook to [realtime-delivery-monitoring]. Order " + deliveryOrderId + " is now in-transit.");
    }
}
