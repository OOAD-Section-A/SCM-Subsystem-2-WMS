package wms.services.integration;

import wms.exceptions.WMSException;
import java.util.List;
import com.scm.subsystems.WarehouseMgmtSubsystem;

public class RFIDAuditorService implements IRFIDAuditorService {

    private final WarehouseMgmtSubsystem exceptions = WarehouseMgmtSubsystem.INSTANCE;

    @Override
    public void auditBin(String binId, String expectedSku, int expectedQty, List<String> scannedTags) throws WMSException {
        System.out.println("RFIDAuditor: Processing bulk RFID scan for Bin: " + binId);
        
        int scannedCount = scannedTags.size();
        
        if (scannedCount == expectedQty) {
            System.out.println("RFIDAuditor: Audit PASS. " + expectedQty + " units of " + expectedSku + " accurately verified in " + binId + ".");
        } else {
            exceptions.onCycleCountDiscrepancy(binId, expectedQty, scannedCount);
            System.err.println("RFIDAuditor: DISCREPANCY DETECTED in " + binId + "!");
            System.err.println(" -> Expected: " + expectedQty + " | Actual Scanned: " + scannedCount);
            // In a real system, this would log to a Discrepancy table.
            throw new WMSException("Cycle count discrepancy in " + binId + ". Expected: " + expectedQty + ", Scanned: " + scannedCount);
        }
    }
}
