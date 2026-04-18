package wms.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: GRASP Information Expert for Pre-Receiving.
 */

public class AdvanceShipmentNotice {
    private String asnId;
    private String poId;
    private Supplier supplier;
    private String expectedArrivalDate;
    private Map<String, ASNItem> items;

    public AdvanceShipmentNotice(String asnId, String poId, Supplier supplier, String expectedArrivalDate) {
        this.asnId = asnId;
        this.poId = poId;
        this.supplier = supplier;
        this.expectedArrivalDate = expectedArrivalDate;
        this.items = new HashMap<>();
    }

    public void addExpectedItem(String sku, int expectedQty) {
        items.put(sku, new ASNItem(this.asnId, sku, expectedQty));
    }

    public String getAsnId() { return asnId; }
    public String getPoId() { return poId; }
    public Supplier getSupplier() { return supplier; }
    public String getExpectedArrivalDate() { return expectedArrivalDate; }
    public Map<String, ASNItem> getItems() { return items; }
}
