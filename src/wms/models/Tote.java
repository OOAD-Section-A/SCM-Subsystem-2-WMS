package wms.models;

/**
 * Description: Reusable plastic bin for mixed store orders. Tracked via Barcode.
 */
public class Tote extends StorageUnit {
    public Tote(String unitId) {
        super(unitId, 15.0); // Totes are for lighter, mixed picks
    }

    @Override
    public String getTrackingMethod() {
        return "1D Barcode [Standard]";
    }
}
