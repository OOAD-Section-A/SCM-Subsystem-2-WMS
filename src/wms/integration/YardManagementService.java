package wms.integration;

import wms.exceptions.WMSException;
import wms.views.WarehouseTerminalView;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages yard operations — dock door assignments and vehicle geofence arrivals.
 *
 * GRASP — Information Expert: owns all dock occupancy state.
 * EXCEPTION POLICY: logs via WMSLogger, takes no corrective action.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 7 (Real-Time Delivery Monitoring):
 * Call handleGeofenceArrival() when a vehicle enters the warehouse geofence.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 17 (Exception Handling):
 * onDockDoubleBooking() is called via WarehouseMgmtSubsystem.INSTANCE when a
 * dock conflict is detected. Accessed defensively via try-catch to handle
 * cases where the JAR is on the compile classpath but not the runtime classpath.
 */
public class YardManagementService implements IYardManagementService {

    // Tracks which shipment is currently occupying each dock door.
    // Key: dockId (e.g., "Dock-B"), Value: shipmentId
    private final Map<String, String> occupiedDocks = new HashMap<>();

    /**
     * Handles a vehicle geofence arrival event from Subsystem 7.
     * Assigns a dock door to the arriving shipment.
     * Throws WMSException if the assigned dock is already occupied.
     *
     * @param shipmentId  Identifier of the arriving shipment
     * @param vehicleId   Vehicle identifier from Subsystem 7 GPS tracking
     * @return            The assigned dock door ID (e.g., "Dock-B")
     */
    @Override
    public String handleGeofenceArrival(String shipmentId, String vehicleId) throws WMSException {
        WarehouseTerminalView.printSystemEvent("YardManagement",
                "GEOFENCE_ENTRY from Subsystem 7 | Vehicle: " + vehicleId
                        + " | Shipment: " + shipmentId);

        String assignedDock = "Dock-B"; // Simulated assignment — real impl queries dock schedule

        if (occupiedDocks.containsKey(assignedDock)) {
            String existingShipment = occupiedDocks.get(assignedDock);

        // Route through SafeExceptionAdapter — the single Subsystem 17 integration point.
        SafeExceptionAdapter.onDockDoubleBooking(assignedDock, existingShipment, shipmentId);

            throw new WMSException("Dock assignment conflict: " + assignedDock
                    + " already occupied by shipment " + existingShipment);
        }

        occupiedDocks.put(assignedDock, shipmentId);
        WarehouseTerminalView.printSystemEvent("YardManagement",
                "Dock assigned: " + assignedDock + " -> Shipment: " + shipmentId);
        return assignedDock;
    }

    /**
     * Pre-populates a dock as occupied for simulation and testing purposes.
     *
     * @param dockId      The dock to mark as occupied
     * @param shipmentId  The shipment currently using that dock
     */
    public void simulateOccupiedDock(String dockId, String shipmentId) {
        occupiedDocks.put(dockId, shipmentId);
        WarehouseTerminalView.printSystemEvent("YardManagement",
                "Simulation: Dock " + dockId + " pre-occupied by " + shipmentId);
    }
}