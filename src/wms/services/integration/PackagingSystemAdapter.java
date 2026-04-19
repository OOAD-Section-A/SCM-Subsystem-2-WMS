package wms.services.integration;

import wms.models.Order;
import wms.models.Product;
import wms.exceptions.WMSException;
import com.scm.packing.integration.warehouse.IWarehousePackingIntegration;
import com.scm.packing.integration.warehouse.PackingRequest;
import com.scm.packing.integration.warehouse.PackingRequestItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PackagingSystemAdapter implements IExternalPackingService {

    private final IWarehousePackingIntegration externalSystem;

    public PackagingSystemAdapter(IWarehousePackingIntegration externalSystem) {
        this.externalSystem = externalSystem;
    }

    @Override
    public String dispatchPackingJob(Order order, Map<String, Product> productCatalog) throws WMSException {
        System.out.println("PackagingSystemAdapter: Converting verified WMS Order " + order.getOrderId()
                + " into a Packaging Subsystem Request.");

        List<PackingRequestItem> requestItems = new ArrayList<>();
        String currentDate = LocalDate.now().toString();

        // Convert WMS Line Items to External PackingRequestItems
        for (Map.Entry<String, Integer> entry : order.getLineItems().entrySet()) {
            String sku = entry.getKey();
            int qty = entry.getValue();

            Product product = productCatalog.get(sku);
            String productName = (product != null) ? product.getName() : "Unknown Product";
            String category = (product != null) ? product.getCategory().name() : "DRY_GOODS";

            // Encode perishability, dates, name, and type into the description field
            String encodedDescription = String.format("%s | Type: %s | Sent Date: %s", productName, category,
                    currentDate);

            // Determine fragility based on category
            boolean isFragile = category.equals("HIGH_VALUE") || sku.contains("GLASS");
            double defaultWeight = 1.0;

            for (int i = 0; i < qty; i++) {
                requestItems.add(new PackingRequestItem(
                        sku + "-" + i,
                        encodedDescription,
                        defaultWeight,
                        isFragile));
            }
        }

        PackingRequest request = new PackingRequest(order.getOrderId(), requestItems);

        System.out.println("PackagingSystemAdapter: Dispatching to external Packaging Subsystem...");
        String jobId = externalSystem.createPackingJob(request);

        System.out
                .println("PackagingSystemAdapter: Successfully created remote Packing Job! External Job ID: " + jobId);
        return jobId;
    }
}
