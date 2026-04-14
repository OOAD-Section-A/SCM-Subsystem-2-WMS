package wms.factories;

import wms.models.*;

/**
 * Description: Creational Pattern (Factory Method).
 * Centralizes the creation logic for storage units to prevent tight coupling.
 */
public class StorageUnitFactory {
    
    public static StorageUnit createStorageUnit(StorageUnitType type, String uniqueId) {
        System.out.println("Factory: Manufacturing a new " + type + " with ID " + uniqueId);
        
        switch (type) {
            case PALLET:
                return new Pallet(uniqueId);
            case CASE:
                return new Case(uniqueId);
            case TOTE:
                return new Tote(uniqueId);
            default:
                throw new IllegalArgumentException("Unknown Storage Unit Type");
        }
    }
}
