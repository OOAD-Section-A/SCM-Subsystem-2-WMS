package wms.models;

/**
 * Description: Represents a line item on a Goods Receipt Note.
 * Tracks what was physically received vs what is actually usable.
 */
public class GRNItem {
    private String grnId;
    private String productId;
    private int receivedQty;
    private int damagedQty;
    private int acceptedQty;

    public GRNItem(String grnId, String productId, int receivedQty, int damagedQty) {
        this.grnId = grnId;
        this.productId = productId;
        this.receivedQty = receivedQty;
        this.damagedQty = damagedQty;
        this.acceptedQty = receivedQty - damagedQty;
    }

    public int getAcceptedQty() { return acceptedQty; }
    public int getDamagedQty() { return damagedQty; }
}