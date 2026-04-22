# WMS Integration Guide
## Subsystem 2 — Warehouse Management System

This is the single reference document for any subsystem integrating with WMS. Read this before writing any integration code.

---

## Quick Reference — What WMS Exposes

| Method | Class | Purpose |
|---|---|---|
| `getStockLevel(sku)` | `WarehouseFacade` | Current stock quantity for a SKU |
| `getWarehouseParameters()` | `WarehouseFacade` | Lead time, safety stock, capacity config |
| `isWarehouseOperational()` | `WarehouseFacade` | Health check before any operation |
| `reserveStockForOrder(sku, qty)` | `WarehouseFacade` | Reserve stock for an outbound order |
| `recordReplenishmentOrder(...)` | `WarehouseFacade` | Push a replenishment PO into WMS |
| `dispatchOrder(orderId, lineItems)` | `WarehouseFacade` | Dispatch outbound order by orderId + map |
| `dispatchOrder(order, strategy)` | `WarehouseFacade` | Dispatch with full Order object + strategy |
| `receiveAndStoreProduct(product, qty)` | `WarehouseFacade` | Receive inbound stock |
| `processInboundScan(barcode, dockId)` | `WarehouseFacade` | Process RFID/barcode scan at dock |
| `getReadyShipments()` | `TMSIntegrationService` | Poll for shipments ready for pickup |
| `getRejectedShipments()` | `TMSIntegrationService` | Poll for rejected shipments |
| `getShipmentDetails(shipmentId)` | `TMSIntegrationService` | Full packing details for one shipment |
| `acknowledgeShipment(shipmentId)` | `TMSIntegrationService` | Remove event from polling queue |

---

## Active Integrations

| Subsystem | Direction | Status | Notes |
|---|---|---|---|
| 11 — RFID / Barcode | Inbound | Live | Reflection-based via `WMSIntegrationService` |
| 14 — Packing, Repairs, Receipt | Outbound | Live | Packing jobs created per completed pick task |
| 15 — Database Design | Shared DB | Live | All WMS DB ops via `WarehouseManagementAdapter` |
| 17 — Exception Handling | Shared | Live | All 12 WMS exceptions wired, Windows Event Viewer |
| 6 — Transport and Logistics | Outbound | Ready | REST polling via `TMSIntegrationService` singleton |
| 8 — Demand Forecasting | Bidirectional | Ready | Reflection-based binding on `WarehouseFacade` |

---

## Integration Details by Subsystem

### Subsystem 6 — Transport and Logistics Management

WMS publishes shipment events that TMS polls. Zero coupling — WMS never imports any TMS class.

**Access the event store:**

```java
import wms.integration.TMSIntegrationService;
import wms.integration.ShipmentReadyEvent;

TMSIntegrationService tms = TMSIntegrationService.getInstance();

// Poll for ready shipments
List<ShipmentReadyEvent> ready = tms.getReadyShipments();

// Poll for rejections
List<ShipmentReadyEvent> rejected = tms.getRejectedShipments();

// Full details for one shipment
ShipmentReadyEvent details = tms.getShipmentDetails("SHIP001");

// Remove from queue after processing
tms.acknowledgeShipment("SHIP001");
```

**Each `ShipmentReadyEvent` contains:**

| Method | Type | Description |
|---|---|---|
| `getShipmentId()` | String | Shipment / packing job ID |
| `getOrderIds()` | List\<String\> | Order IDs in this shipment |
| `getTotalWeightKg()` | double | Total weight for route optimization |
| `getTotalPackages()` | int | Number of physical packages |
| `getBarcode()` | String | Barcode label — null for rejections |
| `getStatus()` | String | `READY_FOR_PICKUP` or `CANNOT_SHIP` |
| `getRejectionReason()` | String | Populated for `CANNOT_SHIP` only |
| `getContents()` | List\<ShipmentItem\> | productId, quantity, description per line |
| `getEventTimestamp()` | LocalDateTime | When the event was created |

**When events fire:**
- `READY_FOR_PICKUP` — fires when gate pass is approved at dispatch
- `CANNOT_SHIP` — fires when QC fails or damaged goods are detected

**Rules:**
- Always call `acknowledgeShipment(id)` after processing — prevents duplicate polling
- Events are in-memory — poll every 30–60 seconds recommended
- WMS never imports any TMS class — integration is strictly one-way

---

### Subsystem 8 — Demand Forecasting

WMS exposes stable method signatures on `WarehouseFacade` for reflection-based binding. These signatures will not change.

**Stable signatures:**

```java
// Call before any operation — defer all ops if this returns false
boolean isWarehouseOperational()

// Stock baseline for replenishment decisions — returns 0 for unknown SKUs
int getStockLevel(String sku)

// Warehouse config for forecast calculations
WarehouseParameters getWarehouseParameters()

// Reserve incoming stock when replenishment decision is made
boolean reserveStockForOrder(String sku, int quantity)

// Push an approved replenishment order into WMS
boolean recordReplenishmentOrder(
    String orderId,
    String sku,
    int quantity,
    String vendorId,
    LocalDate targetDeliveryDate,
    String urgencyLevel,   // CRITICAL / HIGH / MEDIUM / LOW / NONE
    String rationale)

// Dispatch outbound order by orderId — returns false if any line item is short
boolean dispatchOrder(String orderId, Map<String, Integer> lineItems)

// Receive and store inbound stock after replenishment delivery
void receiveAndStoreProduct(wms.models.Product product, int quantity)
```

**`getWarehouseParameters()` return values:**

| Parameter | Value | Description |
|---|---|---|
| `defaultLeadTimeDays` | 7 | Days between order placement and stock arrival |
| `safetyStockMultiplier` | 1.2 | 20% buffer applied on top of forecast demand |
| `reorderThreshold` | 0.20 | Reorder when stock falls below 20% of capacity |
| `minimumOrderQuantity` | 10 | Smallest viable replenishment order |
| `maximumOrderQuantity` | 10000 | Hard ceiling per order |
| `totalCapacity` | 50000 | Total warehouse unit capacity |
| `currentUtilization` | 0.65 | Currently 65% full |

**Urgency level behaviour in `recordReplenishmentOrder`:**
- `LOW` / `MEDIUM` — auto-approved, status set to `AUTO_APPROVED`
- `HIGH` / `CRITICAL` — flagged for manual review, status set to `PENDING_REVIEW`

Please ensure CRITICAL urgency orders are only pushed after manual approval on your side.

---

### Subsystem 11 — Barcode Reader and RFID Tracker

WMS loads `WMSIntegrationService` from the RFID JAR at runtime via reflection. When the RFID database is unreachable, WMS falls back to stub mode automatically — no crash, no manual intervention.

**What must stay stable in your JAR:**

File: `com/nova/rfid/integration/WMSIntegrationService.class`

These signatures must never change:

```java
public static synchronized WMSIntegrationService getInstance()
public ScanRecord submitScan(String rfidTag, String source)
public Product getProductFromScan(String rfidTag, String source)
public List<ScanRecord> getRecentScans(int limit)
public int[] getTodaySummary()
```

**Constructor requirement:** The constructor must catch all failures from `RFIDSystemFacade.getInstance()` and still return a valid instance with `facade = null`. Each method must null-check facade before use. Without this, WMS cannot initialize when inventory classes are absent from WMS classpath.

**Critical rule:** WMS owns all inventory updates. `submitScan()` must never call `AddStockStrategy`. Every stock update goes through WMS exclusively.

**Boot output when live mode is active:**
```
[SYSTEM | RFIDSystemAdapter] LIVE mode active — connected to Subsystem 11 (WMSIntegrationService).
```

**Boot output when stub mode is active (database unreachable):**
```
[WARNING | RFIDSystemAdapter] STUB mode — Subsystem 11 environment not reachable. Reason: ...
```

---

### Subsystem 14 — Packing, Repairs, Receipt Management

WMS creates a packing job automatically for every completed outbound pick task. No action needed from Subsystem 14 beyond providing their JAR.

**What WMS sends:**
- Packing job creation with: task ID, bin ID, product ID, order ID
- Packing job status updates: COMPLETED, FAILED

**Boot output when connected:**
```
[SYSTEM | Subsystem14PackingAdapter] Subsystem 14 (Packing) connected successfully.
```

---

### Subsystem 15 — Database Design

WMS accesses the shared OOAD database exclusively through `WarehouseManagementAdapter`. No direct JDBC or custom SQL is used for shared tables.

**JAR required:** `database-module-1.0.0-SNAPSHOT-standalone.jar`

**Adapter used:** `com.jackfruit.scm.database.adapter.WarehouseManagementAdapter`

**Tables WMS writes via adapter at runtime:**

| Table | When Written |
|---|---|
| `warehouses` | Boot — registers WH-001 |
| `warehouse_zones` | Boot — registers STORAGE, STAGING, RECEIVING zones |
| `bins` | Boot — registers 5 operational bins |
| `goods_receipts` | Inbound receiving via `InboundReceivingController` |
| `stock_records` | After putaway completes |
| `stock_movements` | Every stock transfer |
| `pick_tasks` | After outbound task completion |
| `staging_dispatch` | After gate pass approval |

**Tables seeded from Excel at boot via `DatabaseSeeder`:**
`proc_suppliers`, `products`, `proc_product_supplier`, `proc_purchase_orders`, `proc_po_items`, `proc_asn`, `goods_receipts`, `proc_quality_inspections`, `proc_supplier_invoices`, `proc_invoice_items`, `proc_supplier_payments`, `proc_discrepancies`

**Data source:** `procurement_final_dataset.xlsx` — must be in project root directory.

**Boot output when online:**
```
[SYSTEM | DB-ADAPTER] Subsystem 15 (Database) connected via WarehouseManagementAdapter.
[SYSTEM | DB-ADAPTER] Default warehouse structure registered: WH-001 with 3 zones and 5 bins.
[SYSTEM | BOOT] Subsystem 15 DB adapter status: ONLINE
```

---

### Subsystem 17 — Exception Handling

WMS routes all 12 registered WMS exceptions through `SafeExceptionAdapter` to Subsystem 17. Exceptions appear as blocking popups, in Windows Event Viewer under `SCM-WarehouseMgmt`, and in the Exception Viewer GUI.

**One-time setup — run as Administrator:**
```cmd
reg add "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\EventLog\Application\SCM-WarehouseMgmt" /v EventMessageFile /t REG_SZ /d "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\EventLogMessages.dll" /f
```

**JARs required in `lib/`:**
- `scm-exception-handler-v3.jar` ← committed to repository
- `scm-exception-viewer-gui.jar` ← committed to repository
- `jna-5.18.1.jar` ← get from Subsystem 17 team
- `jna-platform-5.18.1.jar` ← get from Subsystem 17 team

**All 12 registered WMS exceptions:**

| ID | Exception Name | Severity | When Triggered |
|---|---|---|---|
| 13 | INVALID_PURCHASE_ORDER_REFERENCE | MAJOR | GRN references a PO that does not exist |
| 14 | INVALID_PRODUCT_REFERENCE | MAJOR | Task references an unknown product SKU |
| 19 | RETURN_CONDITION_INVALID | MINOR | Returned item has unrecognised condition code |
| 107 | CONCURRENT_UPDATE_CONFLICT | MINOR | Two workers updating the same product simultaneously |
| 152 | INSUFFICIENT_STOCK_FOR_PICK | MAJOR | Pick task quantity exceeds available stock |
| 153 | STOCK_UNDERFLOW | MAJOR | Reservation would push quantity below zero |
| 154 | BIN_CAPACITY_EXCEEDED | MAJOR | Putaway exceeds bin's maximum capacity |
| 155 | BIN_NOT_FOUND_OR_BLOCKED | MAJOR | Task targets a missing or maintenance-blocked bin |
| 156 | DOCK_DOUBLE_BOOKING | MAJOR | Two shipments assigned to the same dock door |
| 313 | CYCLE_COUNT_DISCREPANCY | WARNING | RFID physical count differs from system count |
| 314 | GRN_QTY_MISMATCH | WARNING | Received quantity differs from PO quantity |
| 407 | DAMAGED_GOODS_DETECTED | WARNING | Damage detected during inbound QC or cold chain |

**Launch Exception Viewer GUI:**
```cmd
cd lib
java -cp ".;scm-exception-viewer-gui.jar;scm-exception-handler-v3.jar;jna-5.18.1.jar;jna-platform-5.18.1.jar" com.scm.gui.ExceptionViewerGUI
```

**Run Exception Demo (triggers all 12 exceptions):**
```cmd
mvn exec:java -Dexec.mainClass="wms.demo.ExceptionDemo"
```

---

## Running WMS

### Prerequisites
- JDK 17 or higher on PATH
- Maven 3.9+
- MySQL 8.0 running on localhost
- All JARs in `lib/` (see JAR Inventory section)
- `lib/database.properties` created with your credentials
- `.mvn/jvm.config` created with your credentials

### Credential Files (Gitignored — Create Manually)

**`lib/database.properties`:**
```properties
db.url=jdbc:mysql://localhost:3306/ooad
db.user=root
db.password=YOUR_PASSWORD
```

**`.mvn/jvm.config`:**
```
-Ddb.url=jdbc:mysql://localhost:3306/ooad
-Ddb.username=root
-Ddb.password=YOUR_PASSWORD
```

### Commands
```cmd
# Compile
mvn clean compile

# Run main application
mvn exec:java -Dexec.mainClass="Main"

# Run exception demo
mvn exec:java -Dexec.mainClass="wms.demo.ExceptionDemo"
```

---

## JAR Inventory

| JAR File | Committed | Source |
|---|---|---|
| `scm-exception-handler-v3.jar` | Yes | Subsystem 17 |
| `scm-exception-viewer-gui.jar` | Yes | Subsystem 17 |
| `packing-subsystem-1.0-SNAPSHOT-all.jar` | No — gitignored | Subsystem 14 |
| `database-module-1.0.0-SNAPSHOT-standalone.jar` | No — gitignored | Subsystem 15 |
| `rfid-tracker-jar-with-dependencies.jar` | No — gitignored | Subsystem 11 |
| `jna-5.18.1.jar` | No — gitignored | Subsystem 17 |
| `jna-platform-5.18.1.jar` | No — gitignored | Subsystem 17 |
| `demand-forecasting-1.0-SNAPSHOT.jar` | No — gitignored | Subsystem 8 |

Gitignored JARs must be obtained directly from the respective subsystem teams.

---

## Known Warnings (All Non-Breaking)

| Warning Message | Cause | Action |
|---|---|---|
| `system scope dependency unresolvable` | Local JARs use Maven `system` scope | Cosmetic — ignore |
| `Log4j2 could not find logging implementation` | DB module uses Log4j internally | Cosmetic — ignore |
| `location of system modules not set` | Source target 17 with newer JDK | Cosmetic — ignore |
| `SwingWorker threads did not finish` | Packing subsystem internal thread pool | Not our code — ignore |
| `mysql-cj-abandoned-connection-cleanup lingering` | MySQL connector cleanup thread | Not our code — ignore |
| `Failed to execute INSERT INTO warehouses` | WH-001 already exists on re-run | Handled by try-catch — ignore |

---

## Architecture and Design

### Boot Sequence (Order Is Critical)

| Step | Component | Why This Order |
|---|---|---|
| 1 | Subsystem 14 Packing Adapter | Initialises DB layer factory first |
| 2 | Subsystem 15 DB Adapter | Bootstraps OOAD schema |
| 3 | DatabaseSeeder | Seeds Excel data into fresh schema |
| 4 | Repository layer | SCMDatabaseAdapter + ResilientRepositoryProxy |
| 5 | Subsystem 11 RFID Adapter | Reflection loader — independent |
| 6 | WarehouseFacade | Depends on RFID adapter |
| 7 | ReplenishmentService observer | Registers on InventoryManager |
| 8 | Subsystem 8 Forecast Adapter | Queries facade — facade must exist |
| 9 | Yard and Cross-Docking services | Independent |
| 10 | TMS Integration Service | Independent singleton |
| 11 | InboundReceivingController | Depends on facade |
| 12 | OutboundTaskController | Background thread — must start last |

### Sacred Architecture — Never Modify

These components have specific structural requirements that must not be changed:

- **`DatabaseSeeder`** — FK-ordered inserts, INSERT IGNORE throughout, Excel sheet order matters
- **`SafeExceptionAdapter`** — All JAR calls wrapped in `SwingUtilities.invokeLater` + `Throwable` catch
- **`ResilientRepositoryProxy`** — Circuit breaker with primary + in-memory fallback
- **`OutboundTaskController`** — 4-poll loop, `retryCounts` HashMap, DLQ after exactly 3 failures
- **`IPickingStrategy`** — Two methods: `generatePickList(Order)` + `optimizePickPath(List)`

### Design Patterns Applied

| Pattern | Where Applied |
|---|---|
| Facade | `WarehouseFacade` — single entry point for all WMS operations |
| Adapter | All classes in `wms.integration` package |
| Observer | `ReplenishmentService` observing `InventoryManager` |
| Strategy | `IPickingStrategy` (Wave, Zone), `IPutawayStrategy` (FIFO, ColdChain) |
| Proxy + Circuit Breaker | `ResilientRepositoryProxy` |
| Singleton | `TMSIntegrationService`, `DatabaseConnectionManager` |
| Factory | `StorageUnitFactory`, `DatabaseLayerFactory` |
| Command | `IWarehouseTask`, `CycleCountTask`, `InterleavedTask` |

### Package Structure

```
src/
├── Main.java                    — Composition root, boot sequence
└── wms/
    ├── commands/                — Task command objects (Command pattern)
    ├── contracts/               — IWMSRepository, WarehouseSubsystemBase
    ├── controllers/             — InboundReceivingController, OutboundTaskController
    ├── demo/                    — ExceptionDemo (standalone, all 12 exceptions)
    ├── exceptions/              — WMSException, WmsCoreException and subtypes
    ├── factories/               — StorageUnitFactory
    ├── integration/             — All adapters, TMS service, RFID adapter, DB adapter
    ├── models/                  — Domain models: Product, GRN, Order, WarehouseParameters, etc.
    ├── observers/               — IInventoryObserver, ReplenishmentService
    ├── services/                — WarehouseFacade, InventoryManager, TaskEngine, etc.
    ├── strategies/              — IPickingStrategy, IPutawayStrategy and implementations
    └── views/                   — WarehouseTerminalView (console output)
```
