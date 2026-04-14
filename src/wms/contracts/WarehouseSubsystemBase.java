package wms.contracts;

/**
 * Description: Abstract Bridge for other subsystems (Order Fulfillment, Real-Time Monitoring).
 */

public abstract class WarehouseSubsystemBase {
    
    protected IWMSRepository repository;

    public WarehouseSubsystemBase(IWMSRepository repository) {
        this.repository = repository;
    }

    // Abstract hook for Order Fulfillment (Subsystem 3 & 4)
    public abstract boolean reserveStockForOrder(String sku, int quantity);

    // Abstract hook for Barcode/RFID Tracker (Subsystem 11)
    public abstract void processInboundScan(String barcode, String dockId);
    
    // Common concrete method that can be used by all extending classes
    public String getSubsystemStatus() {
        return "Warehouse Management Subsystem: Active";
    }
}
