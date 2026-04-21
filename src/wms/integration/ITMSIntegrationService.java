package wms.integration;

import java.util.List;

/**
 * REST polling API contract for Subsystem 6 (Transport and Logistics Management).
 *
 * TMS polls these endpoints periodically to consume WMS shipment events.
 * WMS never calls TMS — this is strictly one-way.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 6 (Transport and Logistics Management):
 * Poll these three methods to get all data you need:
 *
 * 1. getReadyShipments()    — shipments packed and ready for carrier pickup
 * 2. getRejectedShipments() — shipments that cannot be dispatched
 * 3. getShipmentDetails(id) — full packing details for a specific shipment
 *
 * All responses are in-memory. Events are cleared after acknowledgement.
 * Poll frequency recommendation: every 30-60 seconds.
 *
 * PATTERN — Facade (single entry point for TMS to consume WMS data)
 * GRASP  — Low Coupling (TMS has no dependency on WMS internals)
 * SOLID  — Interface Segregation (TMS only sees what it needs)
 */
public interface ITMSIntegrationService {

    /**
     * Returns all shipments that are packed and ready for carrier pickup.
     * TMS uses this to schedule vehicle dispatch and carrier assignment.
     *
     * Poll this endpoint to get READY_FOR_PICKUP events.
     * Events remain available until explicitly acknowledged.
     *
     * @return List of ready shipment events, empty list if none pending
     */
    List<ShipmentReadyEvent> getReadyShipments();

    /**
     * Returns all shipments that have been rejected and cannot be dispatched.
     * TMS uses this to cancel carrier bookings and notify customers.
     *
     * Poll this endpoint to get CANNOT_SHIP events.
     *
     * @return List of rejected shipment events, empty list if none
     */
    List<ShipmentReadyEvent> getRejectedShipments();

    /**
     * Returns full packing details for a specific shipment.
     * TMS uses this for weight/dimension data needed for route optimization.
     *
     * @param shipmentId  The shipment ID (matches packing job ID from Subsystem 14)
     * @return            The full shipment event, or null if not found
     */
    ShipmentReadyEvent getShipmentDetails(String shipmentId);

    /**
     * Publishes a shipment ready event internally within WMS.
     * Called by DispatchGateway when gate pass is approved.
     * TMS does not call this — it is WMS-internal only.
     *
     * @param event  The completed shipment ready event
     */
    void publishShipmentReady(ShipmentReadyEvent event);

    /**
     * Publishes a shipment rejection event internally within WMS.
     * Called by ColdChainVerificationService or InboundReceivingController
     * when QC fails or damaged goods are detected.
     * TMS does not call this — it is WMS-internal only.
     *
     * @param event  The rejection event
     */
    void publishShipmentRejection(ShipmentReadyEvent event);

    /**
     * Acknowledges receipt of a shipment event, removing it from the ready queue.
     * TMS calls this after successfully processing a READY_FOR_PICKUP event
     * to prevent duplicate processing on subsequent polls.
     *
     * @param shipmentId  The shipment ID to acknowledge
     */
    void acknowledgeShipment(String shipmentId);
}
