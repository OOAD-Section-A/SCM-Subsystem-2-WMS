package wms.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: Goods Receipt Note. The official record that goods touched the dock.
 */
public class GRN {
    private String grnId;
    private String poId;
    private String status;
    private Map<String, GRNItem> items;

    public GRN(String grnId, String poId) {
        this.grnId = grnId;
        this.poId = poId;
        this.status = "CREATED";
        this.items = new HashMap<>();
    }

    public void addItem(String sku, int receivedQty, int damagedQty) {
        items.put(sku, new GRNItem(this.grnId, sku, receivedQty, damagedQty));
    }

    public String getGrnId() { return grnId; }
    public GRNItem getItem(String sku) { return items.get(sku); }
}
