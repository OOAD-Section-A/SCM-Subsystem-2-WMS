package wms.integration;

import wms.exceptions.WMSException;

/**
 * Handles geofence events from the Real-Time Delivery subsystem.
 */
public interface IYardManagementService {
    /**
     * Assigns a dock door when a truck breaches the arrival geofence.
     * @param shipmentId The inbound shipment ID.
     * @param vehicleId The delivery vehicle ID.
     * @return The assigned dock door ID.
     * @throws WMSException if dock assignment fails or conflicts.
     */
    String handleGeofenceArrival(String shipmentId, String vehicleId) throws WMSException;
}
