package wms.integration;

import wms.models.Order;
import wms.models.Product;
import wms.exceptions.WMSException;
import java.util.List;
import java.util.Map;

// Validates physical RFID outbound scans against a digital Order, then delegates to Packaging.

/**
 * Acts as the gate between Subsystem 11 (Barcode Reader and RFID Tracker)
 * physical scan verification and Subsystem 14 (Packaging, Repairs,
 * Receipt Management) job dispatch.
 */

public interface IPackingVerificationService {
    boolean verifyAndDispatch(Order order, List<String> scannedSkus, Map<String, Product> productCatalog,
            IWarehousePackingIntegration packagingAdapter) throws WMSException;
}
