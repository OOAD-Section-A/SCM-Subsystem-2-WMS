package wms.integration;

import wms.exceptions.WMSException;
import com.scm.subsystems.WarehouseMgmtSubsystem;
import java.util.HashMap;
import java.util.Map;

public class YardManagementService implements IYardManagementService {
    
    private final WarehouseMgmtSubsystem exceptions = WarehouseMgmtSubsystem.INSTANCE;
    private final Map<String, String> occupiedDocks = new HashMap<>();

    @Override
    public String handleGeofenceArrival(String shipmentId, String vehicleId) throws WMSException {
        System.out.println("YardManagement: Received GEOFENCE_ENTRY event from Real-Time Delivery Monitoring for vehicle " + vehicleId);
        
        String assignedDock = "Dock-B"; // Simulated assignment logic

        if (occupiedDocks.containsKey(assignedDock)) {
            String existingShipment = occupiedDocks.get(assignedDock);
            exceptions.onDockDoubleBooking(assignedDock, existingShipment, shipmentId);
            throw new WMSException("Dock assignment conflict for " + assignedDock);
        }

        occupiedDocks.put(assignedDock, shipmentId);
        System.out.println("YardManagement: Successfully assigned " + assignedDock + " to shipment " + shipmentId);
        return assignedDock;
    }
    
    // For simulation testing
    public void simulateOccupiedDock(String dockId, String shipmentId) {
        occupiedDocks.put(dockId, shipmentId);
    }
}
