package wms.models;

/**
 * Description: Abstract base class for all storage containers.
 * Ensures the Dependency Inversion Principle (DIP) is followed.
 */
public abstract class StorageUnit {
    protected String unitId;
    protected double maxWeightCapacityKg;

    public StorageUnit(String unitId, double maxWeightCapacityKg) {
        this.unitId = unitId;
        this.maxWeightCapacityKg = maxWeightCapacityKg;
    }

    public String getUnitId() { return unitId; }
    public double getMaxWeightCapacityKg() { return maxWeightCapacityKg; }

    // Subsystem 11 (Barcode/RFID) will rely on this behavior
    public abstract String getTrackingMethod(); 
}
