package wms.integration;

import wms.exceptions.WMSException;
import wms.services.WMSLogger;
import wms.views.WarehouseTerminalView;

import java.util.List;

/**
 * Validates physical RFID bulk scans against expected bin inventory during cycle counts.
 *
 * Used by CycleCountTask to cross-validate system stock counts against
 * physical RFID scan tallies without shutting down warehouse operations.
 *
 * PATTERN — Command: called from CycleCountTask.execute()
 * GRASP — Information Expert: owns bin audit logic and discrepancy detection.
 * EXCEPTION POLICY: logs via WMSLogger, takes no corrective action.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 11 (Barcode Reader and RFID Tracker):
 * Provide the scannedTags list from a bulk RFID sweep of the bin.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 17 (Exception Handling):
 * onCycleCountDiscrepancy() is called defensively when a mismatch is found.
 */
public class RFIDAuditorService implements IRFIDAuditorService {

    /**
     * Audits a bin by comparing the physical RFID scan count to the expected quantity.
     *
     * @param binId        The bin location being audited (e.g., "ZONE-DRY-BIN-99")
     * @param expectedSku  The SKU expected to be in the bin
     * @param expectedQty  The quantity the system expects to find
     * @param scannedTags  List of RFID tag strings captured during the bulk scan
     * @throws WMSException If the scanned count does not match the expected quantity
     */
    @Override
    public void auditBin(String binId, String expectedSku,
            int expectedQty, List<String> scannedTags) throws WMSException {

        int scannedCount = scannedTags.size();

        WarehouseTerminalView.printSystemEvent("RFIDAuditor",
                "Auditing Bin: " + binId + " | SKU: " + expectedSku
                        + " | Expected: " + expectedQty
                        + " | Scanned: " + scannedCount);

        if (scannedCount == expectedQty) {
            WarehouseTerminalView.printSystemEvent("RFIDAuditor",
                    "Audit PASS — " + expectedQty + " units of "
                            + expectedSku + " verified in " + binId);
            return;
        }

        // Discrepancy detected — notify Subsystem 17 defensively.
        WarehouseTerminalView.printWarning("RFIDAuditor",
                "DISCREPANCY in " + binId + " | Expected: " + expectedQty
                        + " | Scanned: " + scannedCount);

        try {
            com.scm.subsystems.WarehouseMgmtSubsystem.INSTANCE
                    .onCycleCountDiscrepancy(binId, expectedQty, scannedCount);
        } catch (Throwable t) {
            WMSLogger.logError("RFIDAuditorService.auditBin",
                    "Subsystem 17 unavailable for discrepancy notification: " + t.getMessage());
        }

        throw new WMSException("Cycle count discrepancy in " + binId
                + ". Expected: " + expectedQty + ", Scanned: " + scannedCount);
    }
}