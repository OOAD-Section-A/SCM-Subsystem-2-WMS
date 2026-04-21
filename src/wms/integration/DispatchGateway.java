package wms.integration;

import wms.exceptions.WMSException;
import wms.views.WarehouseTerminalView;

import java.util.List;

/**
 * Manages the physical exit gate when a delivery truck departs the warehouse.
 *
 * On successful gate pass, publishes a ShipmentReadyEvent to TMSIntegrationService
 * so Subsystem 6 (Transport and Logistics Management) can poll it for carrier
 * dispatch scheduling.
 *
 * PATTERN — Observer (indirect): publishes to TMSIntegrationService which
 * TMS polls. WMS has zero direct coupling to TMS.
 *
 * GRASP — Controller: coordinates the gate pass workflow.
 * GRASP — Low Coupling: TMS is never imported or referenced here.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 6 (Transport and Logistics Management):
 * After this gate pass fires, poll TMSIntegrationService.getReadyShipments()
 * to retrieve the ShipmentReadyEvent for carrier assignment.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 7 (Real-Time Delivery Monitoring):
 * The webhook notification at the end of processGatePass signals that
 * the delivery is now in-transit and GPS tracking should begin.
 */
public class DispatchGateway implements IDispatchGateway {

    // Reference to TMS event store — singleton, always available
    private final TMSIntegrationService tmsService;

    public DispatchGateway() {
        this.tmsService = TMSIntegrationService.getInstance();
    }

    /**
     * Validates and approves the departure of a delivery truck.
     *
     * Flow:
     * 1. Verify truck is not empty (at least one package scanned)
     * 2. Approve gate pass
     * 3. Publish ShipmentReadyEvent to TMS polling queue
     * 4. Notify Subsystem 7 (Real-Time Delivery Monitoring) via webhook
     *
     * @param deliveryOrderId  The delivery order being dispatched
     * @param scannedSkus      SKUs scanned on the truck at the exit gate
     * @throws WMSException    If the truck is empty — gate pass denied
     */
    @Override
    public void processGatePass(String deliveryOrderId,
                                 List<String> scannedSkus) throws WMSException {
        WarehouseTerminalView.printSystemEvent("DispatchGateway",
                "Scanning truck for Delivery Order: " + deliveryOrderId
                        + " at Exit Gate...");

        if (scannedSkus == null || scannedSkus.isEmpty()) {
            // Publish rejection event so TMS knows this shipment cannot proceed
            ShipmentReadyEvent rejection = new ShipmentReadyEvent(
                    deliveryOrderId,
                    List.of(deliveryOrderId),
                    "EMPTY_TRUCK — No packages verified at exit gate"
            );
            tmsService.publishShipmentRejection(rejection);
            throw new WMSException("Gate Pass Failed! Truck is empty.");
        }

        WarehouseTerminalView.printSystemEvent("DispatchGateway",
                scannedSkus.size() + " packages verified in truck.");
        WarehouseTerminalView.printSystemEvent("DispatchGateway",
                "Gate Pass Approved! Truck dispatched.");

        // Build and publish ShipmentReadyEvent for TMS polling.
        // Weight defaults to 1.0kg per package — real impl would
        // query Subsystem 14's getPackedItemsSummary() for actual weight.
        double estimatedWeight = scannedSkus.size() * 1.0;
        String barcode = "SCM|" + deliveryOrderId + "|"
                + System.currentTimeMillis();

        // Build contents list from scanned SKUs
        List<ShipmentReadyEvent.ShipmentItem> contents = scannedSkus.stream()
                .map(sku -> new ShipmentReadyEvent.ShipmentItem(sku, 1, sku))
                .toList();

        ShipmentReadyEvent readyEvent = new ShipmentReadyEvent(
                deliveryOrderId,
                List.of(deliveryOrderId),
                estimatedWeight,
                scannedSkus.size(),
                barcode,
                contents
        );
        tmsService.publishShipmentReady(readyEvent);

        // Notify Subsystem 7 (Real-Time Delivery Monitoring) — in-transit signal
        WarehouseTerminalView.printSystemEvent("DispatchGateway",
                "--> WMS NOTIFY: Subsystem 7 webhook fired. Order "
                        + deliveryOrderId + " is now IN-TRANSIT.");
    }
}