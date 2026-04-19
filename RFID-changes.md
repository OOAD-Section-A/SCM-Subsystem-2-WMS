# RFID System (Subsystem 11) Integration Changes Required - 

To Team NOVA,

Updates: (ignore if updated changes already pushed)

To align our subsystems, we have updated the WMS to act as the central aggregator. The WMS will now listen for your scans, tally them up at the dock door, and generate the formal GRN to add the stock in bulk. 

### What You Need To Change

Please apply the following changes to `com.nova.rfid.pattern.structural.RFIDSystemFacade.java`:

**1. Remove Direct Inventory Manipulation**
You need to remove the code that automatically calls `AddStockStrategy`. The WMS is now the sole authority for adding stock upon receiving.

Find this block in `submitScan()` and **delete it**:
```java
// Step 7: Notify inventory — each scan = one stock transaction
if (product.getSku() != null && !product.getSku().isBlank()) {
    addStockStrategy.execute(
            product.getSku(), INVENTORY_LOCATION, INVENTORY_SUPPLIER, 1,
            inventoryRepository, invExSource, IssuingPolicy.FIFO
    );
}
```

**2. Remove Inventory Dependencies**
Since you are no longer calling the Inventory subsystem directly, you can clean up your imports and constructor.

**Remove these fields:**
```java
private final AddStockStrategy         addStockStrategy;
private final InventoryRepository      inventoryRepository;
private final InventoryExceptionSource invExSource;
```

**Remove these from the constructor:**
```java
this.invExSource         = new InventoryExceptionSource();
this.inventoryRepository = new InventoryRepository(invExSource);
this.addStockStrategy    = new AddStockStrategy();
```

### How the Integration Works Now
Your application remains the **Information Expert** for scanning. 
1. **Inbound:** When a tag is scanned at the dock, your system will still validate it, look up the product, and save the event to `barcode_rfid_events`. The WMS `WarehouseFacade` will consume these scans, tally them up, and process the bulk quantities into a GRN once unloading is complete.
2. **Outbound Packing:** For outbound scans, WMS will intercept the tag scans, verify them against the customer's digital `Order`, and securely hand off the verified items to the external Packaging Subsystem.

Thank you! Let us know if you need any clarification on the new flow.
