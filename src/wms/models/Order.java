package wms.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: Represents an outbound fulfillment request from Subsystem 4.
 */
public class Order {
    private String orderId;
    // Maps SKU -> Quantity required
    private Map<String, Integer> lineItems;

    public Order(String orderId) {
        this.orderId = orderId;
        this.lineItems = new HashMap<>();
    }

    public void addItem(String sku, int quantity) {
        lineItems.put(sku, quantity);
    }

    public String getOrderId() { return orderId; }
    public Map<String, Integer> getLineItems() { return lineItems; }
}
