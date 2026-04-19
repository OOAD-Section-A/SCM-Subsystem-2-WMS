package wms.services.integration;

import wms.models.Order;
import wms.models.Product;
import wms.exceptions.WMSException;
import java.util.Map;

//Interface for delegating packing logic to external subsystems.

public interface IExternalPackingService {

    String dispatchPackingJob(Order order, Map<String, Product> productCatalog) throws WMSException;
}
