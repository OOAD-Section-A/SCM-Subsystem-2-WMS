# WMS & RFID Integration Documentation

Please note that the Integrations Directory holds classes/interfaces for necessary integration, internal logic for some is left to the concerned sub systems. 

These were made to adhere with the guidlines sent by sir on the interaction between RFID and WMS across other subsystems. 

## 1. Architectural Philosophy
We implemented all RFID integration points as isolated interfaces inside the `wms.services.integration` package. This allows WMS to process external RFID events without polluting core WMS engines (like `TaskEngine` or `OrderPickingEngine`).

## 2. The 5 Integration Workflows

### A. Inbound Receiving & GRN Batching
* **Logic:** RFID tags are scanned individually as a truck is unloaded. WMS (`WarehouseFacade`) aggregates these scans via `processInboundScan`. Once complete, the `InboundReceivingController` fetches the tallied data and generates a bulk Goods Receipt Note (GRN), which is then passed to Inventory.
* **Exception Handling:** If the tallied RFID count does not match the Expected Quantity on the Advance Shipment Notice (ASN), the `WarehouseMgmtSubsystem` throws a `GRN_QTY_MISMATCH` exception. If damaged items are detected during this stage, it throws `DAMAGED_GOODS_DETECTED`.

### B. Outbound Packing (WMS RFID Verification + Packaging System Handoff)
* **Logic:** 
  1. A warehouse worker scans the RFID tags of items as they are placed into an outbound box.
  2. The `PackingVerificationService` takes the digital `Order` and the list of physical `scannedSkus` to guarantee a 100% match.
  3. If the verification succeeds, the WMS uses the `PackagingSystemAdapter` to compile a `PackingRequest`. It safely encodes the Product Name, Type, Perishability, and Dates into the `description` string, and maps Fragility to the boolean field, before sending it to the external `IWarehousePackingIntegration`.
* **Benefit:** WMS remains the central brain, bridging physical hardware (RFID scans) with the external Packaging software, without breaking their strict API constraints.

### C. Cross-Docking Automation
* **Logic:** When an inbound item is scanned, the `CrossDockingService` immediately checks if that SKU is backordered or flagged as urgent. If true, the WMS bypasses the Putaway storage strategies entirely and routes the item directly to the outbound shipping dock.

### D. Dispatch & Gate Pass
* **Logic:** As a loaded delivery truck exits the warehouse gate, an overarching RFID scanner sweeps the entire truck. The `DispatchGateway` verifies the payload matches the `DeliveryOrder`.
* **External Integration:** Successfully passing this gate triggers an event (simulated via webhook) to the **Realtime Monitoring Subsystem**, instantly starting the GPS tracking for the customer.

### E. Returns Management
* **Logic:** When a delivery agent returns a package, it is scanned at the reverse logistics dock. The `ReturnsManagementService` identifies the original `DeliveryOrder` and the reason for rejection, routing the item for Quality Control inspection before it can re-enter inventory.

### F. Automated Cycle Counting
* **Logic:** To perform inventory audits without halting warehouse operations, a worker uses a mobile RFID scanner to sweep a specific bin. The `RFIDAuditorService` compares the scanned bulk tags against the WMS `InventoryManager` ledger.
* **Exception Handling:** If the scanned count differs from the system's expected count, the central Exception Handler logs a `CYCLE_COUNT_DISCREPANCY` (Warning).

### G. Yard Management (Geofencing)
* **Logic:** The Real-Time Delivery Monitoring subsystem tracks delivery vehicles via GPS. When an inbound truck crosses the warehouse perimeter, it fires a `GEOFENCE_ENTRY` event. The `YardManagementService` listens to this event and automatically assigns an available dock door for unloading.
* **Exception Handling:** If WMS accidentally assigns a dock door that is already occupied, the `WarehouseMgmtSubsystem` fires a `DOCK_DBL_BOOKING` (Major) exception.

### H. Cold Chain Verification (RFID + Real-Time)
* **Logic:** Perishable goods (like milk) require strict temperature control. During transit, the Real-Time Delivery system monitors the truck. Upon arrival, the RFID tags (equipped with temp sensors) are scanned at the dock. The `ColdChainVerificationService` cross-references the RFID arrival temperature with the Real-Time transit history.
* **Exception Handling:** If the temperature exceeded safe limits at *any* point, the `WarehouseMgmtSubsystem` throws a `DAMAGED_GOODS_DETECTED` (Warning) exception, and the goods are rejected.
