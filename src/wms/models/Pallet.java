package wms.models;

/**
 * Description: Heavy-duty wooden/plastic base for bulk goods. Tracked via RFID.
 */
public class Pallet extends StorageUnit {
    public Pallet(String unitId) {
        super(unitId, 1000.0); // Pallets can hold up to 1000kg
    }

    @Override
    public String getTrackingMethod() {
        return "RFID Tag [High-Range]";
    }
}
