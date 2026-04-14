package wms.contracts;

/**
 * Description: Contract for the Database Design team. 
 * The WMS subsystem will call these methods, and the DB team must implement them.
 * This ensures no direct SQL injection or tight coupling.
 */
public interface IWMSRepository {
    
    // Validates if a PO exists for inbound receiving
    boolean validatePurchaseOrder(String poNumber);
    
    // Updates stock movement for Double-entry Stock Keeping
    void recordStockMovement(String sku, String fromLocationId, String toLocationId, int quantity);
    
    // Checks raw database for bin availability
    boolean isBinAvailable(String binId);
}