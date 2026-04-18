package wms.models;

/**
 * Description: Represents a line item on a Purchase Order.
 * Tracks ordered, received, and pending quantities for partial deliveries.
 */

public class POItem {
    private String poId;
    private String productId;
    private int orderedQty;
    private int receivedQty;
    private int pendingQty;
    private double agreedPrice;

    public POItem(String poId, String productId, int orderedQty, double agreedPrice) {
        this.poId = poId;
        this.productId = productId;
        this.orderedQty = orderedQty;
        this.receivedQty = 0;
        this.pendingQty = orderedQty;
        this.agreedPrice = agreedPrice;
    }

    public String getProductId() { return productId; }
    public int getOrderedQty() { return orderedQty; }
    public int getReceivedQty() { return receivedQty; }
    public int getPendingQty() { return pendingQty; }
    public double getAgreedPrice() { return agreedPrice; }

    public void addReceivedQty(int qty) {
        this.receivedQty += qty;
        this.pendingQty = this.orderedQty - this.receivedQty;
    }
}
