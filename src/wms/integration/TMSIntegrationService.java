package wms.integration;

import wms.views.WarehouseTerminalView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the TMS REST polling integration service.
 *
 * Holds shipment events in memory. TMS polls getReadyShipments() and
 * getRejectedShipments() periodically to consume WMS events without
 * any direct coupling between the two subsystems.
 *
 * PATTERN — Singleton: one event store per WMS instance ensures
 * TMS always polls the same authoritative source.
 *
 * PATTERN — Facade: single entry point for all TMS data needs.
 *
 * GRASP — Information Expert: this class owns shipment event state
 * because it is the natural place where that information is created.
 *
 * GRASP — Pure Fabrication: exists purely to serve the integration
 * contract — does not map to a real-world warehouse entity.
 *
 * THREAD SAFETY: Uses synchronized collections since OutboundTaskController
 * publishes events from a background thread while TMS polls from another.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 6 (Transport and Logistics Management):
 * Obtain this singleton via TMSIntegrationService.getInstance() and call:
 *   getReadyShipments()    — poll for ready shipments
 *   getRejectedShipments() — poll for rejections
 *   getShipmentDetails(id) — get full details for one shipment
 *   acknowledgeShipment(id)— confirm you processed it (removes from queue)
 */
public class TMSIntegrationService implements ITMSIntegrationService {

    // Singleton instance
    private static TMSIntegrationService instance;

    // Ready shipments waiting for TMS carrier pickup
    // LinkedHashMap preserves insertion order for FIFO polling
    private final Map<String, ShipmentReadyEvent> readyShipments =
            Collections.synchronizedMap(new LinkedHashMap<>());

    // Rejected shipments TMS must cancel
    private final Map<String, ShipmentReadyEvent> rejectedShipments =
            Collections.synchronizedMap(new LinkedHashMap<>());

    private TMSIntegrationService() {}

    /** Returns the singleton instance. Thread-safe double-checked locking. */
    public static synchronized TMSIntegrationService getInstance() {
        if (instance == null) {
            instance = new TMSIntegrationService();
        }
        return instance;
    }

    /**
     * Returns all READY_FOR_PICKUP events currently waiting for TMS.
     * Returns a snapshot — safe for TMS to iterate without locking.
     */
    @Override
    public List<ShipmentReadyEvent> getReadyShipments() {
        synchronized (readyShipments) {
            return new ArrayList<>(readyShipments.values());
        }
    }

    /**
     * Returns all CANNOT_SHIP events currently waiting for TMS.
     */
    @Override
    public List<ShipmentReadyEvent> getRejectedShipments() {
        synchronized (rejectedShipments) {
            return new ArrayList<>(rejectedShipments.values());
        }
    }

    /**
     * Returns full event details for a specific shipment ID.
     * Checks both ready and rejected queues.
     */
    @Override
    public ShipmentReadyEvent getShipmentDetails(String shipmentId) {
        ShipmentReadyEvent event = readyShipments.get(shipmentId);
        if (event == null) {
            event = rejectedShipments.get(shipmentId);
        }
        return event;
    }

    /**
     * Stores a READY_FOR_PICKUP event for TMS to poll.
     * Called by DispatchGateway after gate pass is approved.
     */
    @Override
    public void publishShipmentReady(ShipmentReadyEvent event) {
        readyShipments.put(event.getShipmentId(), event);
        WarehouseTerminalView.printSystemEvent("TMS-INTEGRATION",
                "Shipment READY published for TMS polling | ID: "
                        + event.getShipmentId()
                        + " | Orders: " + event.getOrderIds()
                        + " | Weight: " + event.getTotalWeightKg() + "kg"
                        + " | Barcode: " + event.getBarcode());
    }

    /**
     * Stores a CANNOT_SHIP rejection event for TMS to poll.
     * Called by ColdChainVerificationService or InboundReceivingController.
     */
    @Override
    public void publishShipmentRejection(ShipmentReadyEvent event) {
        rejectedShipments.put(event.getShipmentId(), event);
        WarehouseTerminalView.printSystemEvent("TMS-INTEGRATION",
                "Shipment REJECTED published for TMS polling | ID: "
                        + event.getShipmentId()
                        + " | Reason: " + event.getRejectionReason());
    }

    /**
     * Removes a shipment from the ready queue after TMS confirms receipt.
     * Prevents duplicate processing on subsequent TMS polls.
     */
    @Override
    public void acknowledgeShipment(String shipmentId) {
        boolean removed = readyShipments.remove(shipmentId) != null
                || rejectedShipments.remove(shipmentId) != null;
        if (removed) {
            WarehouseTerminalView.printSystemEvent("TMS-INTEGRATION",
                    "Shipment acknowledged and removed from polling queue: "
                            + shipmentId);
        }
    }

    /**
     * Returns a summary of current pending events.
     * Used by Main.java for boot status reporting.
     */
    public String getQueueStatus() {
        return "TMS event queue | Ready: " + readyShipments.size()
                + " | Rejected: " + rejectedShipments.size();
    }
}
