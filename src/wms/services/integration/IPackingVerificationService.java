package wms.services.integration;

import wms.models.Order;
import wms.exceptions.WMSException;
import java.util.List;

/**
 * Interface defining how the WMS validates RFID packing scans.
 * Ensures the physical items packed match the digital Order.
 */
public interface IPackingVerificationService {
    /**
     * Verifies that the scanned SKUs perfectly match the expected Order line items.
     * @param order The outbound Order being fulfilled.
     * @param scannedSkus The list of SKUs read by the RFID scanner at the packing station.
     * @return true if valid and packed.
     * @throws WMSException if there is a mismatch (missing or extra items).
     */
    boolean verifyPacking(Order order, List<String> scannedSkus) throws WMSException;
}
