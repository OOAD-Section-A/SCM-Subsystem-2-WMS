package wms.models;

/**
 * Description: Represents a line item on an Advance Shipment Notice.
 */

public class ASNItem {
    private String asnId;
    private String productId;
    private int expectedQty;

    public ASNItem(String asnId, String productId, int expectedQty) {
        this.asnId = asnId;
        this.productId = productId;
        this.expectedQty = expectedQty;
    }

    public String getProductId() { return productId; }
    public int getExpectedQty() { return expectedQty; }
}