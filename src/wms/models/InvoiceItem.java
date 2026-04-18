package wms.models;

/**
 * Description: Represents a billed line item on a Supplier Invoice.
 */
public class InvoiceItem {
    private String invoiceId;
    private String productId;
    private int billedQty;
    private double billedPrice;

    public InvoiceItem(String invoiceId, String productId, int billedQty, double billedPrice) {
        this.invoiceId = invoiceId;
        this.productId = productId;
        this.billedQty = billedQty;
        this.billedPrice = billedPrice;
    }

    public String getProductId() { return productId; }
    public int getBilledQty() { return billedQty; }
    public double getBilledPrice() { return billedPrice; }
}
