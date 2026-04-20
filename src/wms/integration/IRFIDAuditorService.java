package wms.services.integration;

import wms.exceptions.WMSException;
import java.util.List;

/**
 * Defines how WMS processes bulk RFID scans for cycle counts.
 */
public interface IRFIDAuditorService {
    /**
     * Audits a specific bin against physical RFID scans.
     * @param binId The storage bin location.
     * @param expectedSku The SKU that should be in the bin.
     * @param expectedQty The quantity expected according to WMS.
     * @param scannedTags The list of RFID tags scanned in the bin.
     * @throws WMSException if discrepancies are found.
     */
    void auditBin(String binId, String expectedSku, int expectedQty, List<String> scannedTags) throws WMSException;
}
