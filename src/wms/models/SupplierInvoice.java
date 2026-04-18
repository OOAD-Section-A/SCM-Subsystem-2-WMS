package wms.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: The official bill from the supplier.
 */
public class SupplierInvoice {
    private String invoiceId;
    private String poId;
    private Map<String, InvoiceItem> items;

    public SupplierInvoice(String invoiceId, String poId) {
        this.invoiceId = invoiceId;
        this.poId = poId;
        this.items = new HashMap<>();
    }

    public void addItem(String sku, int billedQty, double billedPrice) {
        items.put(sku, new InvoiceItem(this.invoiceId, sku, billedQty, billedPrice));
    }

    public String getInvoiceId() { return invoiceId; }
    public Map<String, InvoiceItem> getItems() { return items; }
}