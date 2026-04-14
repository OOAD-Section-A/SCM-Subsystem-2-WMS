package wms.models;

/**
 * Description: Standard cardboard box for grouped items. Tracked via Barcode.
 */
public class Case extends StorageUnit {
    public Case(String unitId) {
        super(unitId, 25.0); // Cases hold up to 25kg safely
    }

    @Override
    public String getTrackingMethod() {
        return "2D Barcode [Standard]";
    }
}
