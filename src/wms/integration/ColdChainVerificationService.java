package wms.integration;

import wms.exceptions.WMSException;
import wms.views.WarehouseTerminalView;

/**
 * Verifies cold chain integrity for perishable goods at dock arrival.
 *
 * Two failure paths:
 * 1. Transit breach reported by Subsystem 7 (Real-Time Delivery Monitoring)
 * 2. RFID temperature sensor reading exceeds MAX_SAFE_TEMP at the dock
 *
 * Both failures notify Subsystem 17 (Exception Handling) defensively —
 * the JAR call is wrapped in try-catch to handle runtime classpath absence.
 *
 * EXCEPTION POLICY: logs via WMSLogger, takes no corrective action.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 7 (Real-Time Delivery Monitoring):
 * Set transitBreachAlert=true if a breach was detected during transit.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 11 (Barcode Reader and RFID Tracker):
 * Pass the rfidTempReading from the temperature-enabled RFID tag sensor.
 */
public class ColdChainVerificationService implements IColdChainVerificationService {

    // Maximum safe arrival temperature for perishable cold-chain goods.
    private static final double MAX_SAFE_TEMP = 4.0; // Celsius

    /**
     * Verifies temperature compliance for a perishable SKU arriving at a dock.
     *
     * @param sku                The SKU of the perishable product being verified
     * @param dockId             The dock where the product is arriving
     * @param rfidTempReading    Temperature reading from the RFID tag sensor (Celsius)
     * @param transitBreachAlert True if Subsystem 7 reported a breach during transit
     * @throws WMSException      If cold chain is broken — caller must handle disposition
     */
    @Override
    public void verifyTemperatureHandoff(String sku, String dockId,
            double rfidTempReading, boolean transitBreachAlert) throws WMSException {

        WarehouseTerminalView.printSystemEvent("ColdChainVerification",
                "Checking temperature for SKU: " + sku + " at " + dockId
                        + " | RFID Reading: " + rfidTempReading + "C");

        // Path 1: Transit breach reported by Subsystem 7.
        if (transitBreachAlert) {
            notifySubsystem17DamagedGoods(sku, dockId);
            throw new WMSException(
                    "Cold Chain Broken: Transit breach alert active for " + sku);
        }

        // Path 2: RFID dock sensor reading exceeds safe threshold.
        if (rfidTempReading > MAX_SAFE_TEMP) {
            WarehouseTerminalView.printWarning("ColdChainVerification",
                    "Temperature breach: " + rfidTempReading + "C exceeds max "
                            + MAX_SAFE_TEMP + "C for SKU: " + sku);
            notifySubsystem17DamagedGoods(sku, dockId);
            throw new WMSException(
                    "Cold Chain Broken: Arrival temperature too high for " + sku
                            + " (" + rfidTempReading + "C > " + MAX_SAFE_TEMP + "C)");
        }

        WarehouseTerminalView.printSystemEvent("ColdChainVerification",
                "SUCCESS — Cold chain maintained for SKU: " + sku
                        + " | Temp: " + rfidTempReading + "C (within limit)");
    }

    private void notifySubsystem17DamagedGoods(String sku, String dockId) {
    // Route through SafeExceptionAdapter — the single Subsystem 17 integration point.
    SafeExceptionAdapter.onDamagedGoodsDetected(sku, dockId);
    }
}
