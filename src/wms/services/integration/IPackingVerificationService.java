package wms.services.integration;

import wms.models.Order;
import wms.models.Product;
import wms.exceptions.WMSException;
import java.util.List;
import java.util.Map;

/**
 * Validates physical RFID outbound scans against a digital Order, then delegates to Packaging.
 */
public interface IPackingVerificationService {
    boolean verifyAndDispatch(Order order, List<String> scannedSkus, Map<String, Product> productCatalog, IExternalPackingService packagingAdapter) throws WMSException;
}
