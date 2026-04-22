# Warehouse Management System (WMS)
### SCM Project — Team 13 | Subsystem 2

**Course:** Object Oriented Analysis & Design (OOAD) — UE23CS352B  
**Faculty:** Anand M S  
**Institution:** PES University

---

## Team Members

| Name | SRN |
|---|---|
| Aaditya Singh | PES1UG23AM002 |
| Aaron Sabu | PES1UG23AM004 |
| Aashlesh Lokesh | PES1UG23AM007 |
| Adyansh Aggarwal | PES1UG23AM028 |

---

## Project Overview

This repository contains Subsystem 2 of a multi-team Supply Chain Management (SCM) application built for the OOAD course. The system models a real-world Warehouse Management System that handles the complete lifecycle of goods — from inbound receiving at the dock to outbound dispatch at the gate.

The WMS integrates with six other subsystems: RFID/Barcode (11), Packing (14), Database Design (15), Exception Handling (17), Transport and Logistics (6), and Demand Forecasting (8). All integrations are designed with zero tight coupling — every subsystem can evolve independently without breaking the WMS.

---

## Features Implemented

### Warehouse Operations

| Feature | Description |
|---|---|
| **Inbound Receiving** | Full dock-to-shelf pipeline: ASN validation, GRN creation, QC inspection, and putaway strategy selection |
| **Putaway Strategies** | Standard FIFO and Cold Chain putaway — selected automatically based on product category |
| **Outbound Task Engine** | Wave picking and zone picking strategies with a background poller, 3-retry DLQ, and Subsystem 17 escalation on failure |
| **Inventory Management** | Stock ledger with concurrent-safe updates, stock reservation, and add/deduct operations |
| **Replenishment Observer** | Automatic low-stock alerts when inventory drops below configured safety thresholds |
| **Cross-Docking** | Bypasses putaway for urgent backorder items — routes directly from receiving dock to shipping dock |
| **Yard Management** | Dock door assignment, geofence arrival handling, and dock conflict detection |
| **Cycle Counts** | RFID-driven bin audits with discrepancy detection and Subsystem 17 escalation |
| **Cold Chain Verification** | Temperature monitoring and damaged goods detection during inbound QC |
| **Returns Management** | Handles return condition validation and disposition routing |
| **Slotting Optimizer** | Uses Demand Forecasting signals to optimize bin assignments for fast-moving SKUs |
| **Storage Units** | Pallet, tote, and case tracking using the Factory pattern |
| **RFID Auditor** | Periodic RFID-based stock verification against the system ledger |
| **Dispatch Gateway** | Gate scanner validation on truck departure, publishes shipment events for TMS |
| **TMS Event Publishing** | Publishes `READY_FOR_PICKUP` and `CANNOT_SHIP` events for Transport and Logistics to poll |

### Procurement and Vendor Management

| Feature | Description |
|---|---|
| **Supplier Master** | 20 suppliers seeded from Excel with reliability scores and lead time data |
| **Product Catalog** | 100 products seeded with category, SKU, unit of measure, and storage type |
| **Product-Supplier Links** | 318 supplier-product pricing relationships |
| **Purchase Orders** | 400 POs seeded with priority, status, and warehouse assignment |
| **PO Line Items** | 1786 PO items with ordered and received quantities |
| **Advance Shipment Notices** | 300 ASNs linking incoming deliveries to purchase orders |
| **Goods Receipts** | 300 GRN headers recording inbound deliveries |
| **Quality Inspections** | 800 QC inspection records with pass/fail quantities |
| **Supplier Invoices** | 300 invoice headers for 3-way matching |
| **Invoice Line Items** | 1216 invoice line items |
| **Supplier Payments** | 300 payment records with PENDING/COMPLETED status |
| **Discrepancy Tracking** | 200 procurement discrepancy records (quantity, price, damage) |
| **Vendor Selection Engine** | Selects optimal vendor based on lead time, reliability, and price |
| **Procurement Service** | Manages the full PO lifecycle within WMS |

### Exception Handling

All 12 WMS-registered exceptions are wired to Subsystem 17 and appear as blocking popups, in Windows Event Viewer under `SCM-WarehouseMgmt`, and in the Exception Viewer GUI.

| ID | Exception | Severity |
|---|---|---|
| 13 | INVALID_PURCHASE_ORDER_REFERENCE | MAJOR |
| 14 | INVALID_PRODUCT_REFERENCE | MAJOR |
| 19 | RETURN_CONDITION_INVALID | MINOR |
| 107 | CONCURRENT_UPDATE_CONFLICT | MINOR |
| 152 | INSUFFICIENT_STOCK_FOR_PICK | MAJOR |
| 153 | STOCK_UNDERFLOW | MAJOR |
| 154 | BIN_CAPACITY_EXCEEDED | MAJOR |
| 155 | BIN_NOT_FOUND_OR_BLOCKED | MAJOR |
| 156 | DOCK_DOUBLE_BOOKING | MAJOR |
| 313 | CYCLE_COUNT_DISCREPANCY | WARNING |
| 314 | GRN_QTY_MISMATCH | WARNING |
| 407 | DAMAGED_GOODS_DETECTED | WARNING |

### Design Patterns Applied

| Pattern | Where Applied |
|---|---|
| Facade | `WarehouseFacade` — single entry point for all WMS operations |
| Adapter | All integration classes in `wms.integration` |
| Observer | `ReplenishmentService` observing `InventoryManager` |
| Strategy | `IPickingStrategy` (Wave, Zone), `IPutawayStrategy` (FIFO, ColdChain) |
| Proxy + Circuit Breaker | `ResilientRepositoryProxy` |
| Singleton | `TMSIntegrationService`, `DatabaseConnectionManager` |
| Factory | `StorageUnitFactory`, `DatabaseLayerFactory` |
| Command | `IWarehouseTask`, `CycleCountTask`, `InterleavedTask` |

---

## Integrations

WMS integrates with six subsystems. All integrations use Anti-Corruption Layer adapters — no subsystem imports WMS internals directly and WMS does not import other subsystem internals.

| Subsystem | Direction | Status |
|---|---|---|
| 11 — RFID / Barcode | Inbound | Live — reflection-based dual live/stub mode |
| 14 — Packing, Repairs, Receipt | Outbound | Live — packing jobs per completed pick task |
| 15 — Database Design | Shared DB | Live — all WMS ops via `WarehouseManagementAdapter` |
| 17 — Exception Handling | Shared | Live — all 12 WMS exceptions wired |
| 6 — Transport and Logistics | Outbound | Ready — REST polling via `TMSIntegrationService` |
| 8 — Demand Forecasting | Bidirectional | Ready — reflection-based facade API |

For detailed integration instructions, API contracts, method signatures, and integration notes for each subsystem, see **[INTEGRATION.md](INTEGRATION.md)**.

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 or higher | Tested on JDK 17 and JDK 25 |
| Maven | 3.9+ | Used for build and run |
| MySQL | 8.0 | Must be running on localhost:3306 |
| Git | Any | For cloning the repository |

---

## Setup Instructions

### Step 1 — Clone the Repository

```cmd
git clone https://github.com/OOAD-Section-A/SCM-Subsystem-2-WMS.git SCM_Project
cd SCM_Project
```

### Step 2 — Obtain Gitignored JARs

The following JARs are excluded from the repository due to file size. Obtain them from the respective teams and place them in the `lib/` directory:

| JAR | Source |
|---|---|
| `packing-subsystem-1.0-SNAPSHOT-all.jar` | Subsystem 14 team |
| `database-module-1.0.0-SNAPSHOT-standalone.jar` | Subsystem 15 team |
| `rfid-tracker-jar-with-dependencies.jar` | Subsystem 11 team |
| `jna-5.18.1.jar` | Subsystem 17 team |
| `jna-platform-5.18.1.jar` | Subsystem 17 team |
| `demand-forecasting-1.0-SNAPSHOT.jar` | Subsystem 8 team |

The following JARs are already committed in the repository:
- `lib/scm-exception-handler-v3.jar`
- `lib/scm-exception-viewer-gui.jar`

### Step 3 — Create Database Credential Files

These files are gitignored and must be created manually.

**Create `lib/database.properties`:**
```properties
db.url=jdbc:mysql://localhost:3306/ooad
db.user=root
db.password=YOUR_MYSQL_PASSWORD
```

**Create `.mvn/jvm.config`:**
```
-Ddb.url=jdbc:mysql://localhost:3306/ooad
-Ddb.username=root
-Ddb.password=YOUR_MYSQL_PASSWORD
```

### Step 4 — Register Windows Event Viewer Source

Run once as Administrator. Required for Subsystem 17 exception logging to work:

```cmd
reg add "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\EventLog\Application\SCM-WarehouseMgmt" /v EventMessageFile /t REG_SZ /d "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\EventLogMessages.dll" /f
```

### Step 5 — Verify MySQL is Running

```cmd
mysql -u root -p
```

Enter your password. If you see the MySQL prompt, the database is ready.
Type `exit` to return to the terminal.

### Step 6 — Compile

```cmd
mvn clean compile
```

Expected: `BUILD SUCCESS` — 85 source files compiled.

---

## Running the Project

### Run the Main Application

```cmd
mvn exec:java -Dexec.mainClass="Main"
```

The application will:
1. Connect to Packing, Database, and RFID subsystems
2. Bootstrap the OOAD schema via `SupplyChainDatabaseFacade`
3. Seed all 14 Excel sheets into MySQL
4. Start the outbound task poller on a background thread
5. Print boot confirmation for every subsystem

**Expected boot output:**
```
[SYSTEM | DB-ADAPTER] Subsystem 15 (Database) connected via WarehouseManagementAdapter.
[SYSTEM | DATABASE] Connection established successfully.
[SYSTEM | DB SEEDER] Excel data successfully injected into MySQL!
[SYSTEM | RFIDSystemAdapter] LIVE mode active — connected to Subsystem 11 (WMSIntegrationService).
[SYSTEM | BOOT] TMS Integration Service ready. TMS event queue | Ready: 0 | Rejected: 0
[SYSTEM | BOOT] WMS Subsystem fully initialized and awaiting integration triggers.
```

### Run the Exception Demo

Triggers all 12 WMS exceptions to demonstrate Subsystem 17 integration.
Launch the Exception Viewer GUI first (see below), then run the demo.

```cmd
mvn exec:java -Dexec.mainClass="wms.demo.ExceptionDemo"
```

Each exception fires a blocking popup — click OK to proceed to the next one.
After all 12, click **Refresh Now** in the GUI.

### Launch the Exception Viewer GUI

Run from the `lib/` directory in a separate terminal window:

```cmd
cd lib
java -cp ".;scm-exception-viewer-gui.jar;scm-exception-handler-v3.jar;jna-5.18.1.jar;jna-platform-5.18.1.jar" com.scm.gui.ExceptionViewerGUI
```

On first launch, select `Warehouse Mgmt` from the subsystem dropdown.
The GUI auto-refreshes every 30 seconds and has a manual **Refresh Now** button.

### Build the Integration JAR

Rebuilds `dist/wms-subsystem-2.jar` after source changes:

```cmd
mvn clean package -DskipTests
copy target\warehouse-management-1.0-SNAPSHOT.jar dist\wms-subsystem-2.jar
```

### Verify Database Contents

After running the application, open MySQL Workbench and run:

```sql
USE ooad;
SELECT COUNT(*) FROM proc_suppliers;        -- Expected: 21
SELECT COUNT(*) FROM products;              -- Expected: 100
SELECT COUNT(*) FROM proc_product_supplier; -- Expected: 318
SELECT COUNT(*) FROM proc_purchase_orders;  -- Expected: 400
SELECT COUNT(*) FROM proc_po_items;         -- Expected: 1786
SELECT COUNT(*) FROM proc_asn;              -- Expected: 300
SELECT COUNT(*) FROM goods_receipts;        -- Expected: 300
SELECT COUNT(*) FROM proc_quality_inspections; -- Expected: 800
SELECT COUNT(*) FROM proc_supplier_invoices;   -- Expected: 300
SELECT COUNT(*) FROM proc_invoice_items;       -- Expected: 1216
SELECT COUNT(*) FROM proc_supplier_payments;   -- Expected: 300
SELECT COUNT(*) FROM proc_discrepancies;       -- Expected: 200
```

---

## Directory Structure

```
SCM-Subsystem-2-WMS/
│
├── README.md                          — This file
├── INTEGRATION.md                     — Full integration guide for all 6 subsystems
├── pom.xml                            — Maven build descriptor
├── procurement_final_dataset.xlsx     — Seed data: 14 sheets, 12 DB tables, 6000+ rows
│
├── dist/
│   └── wms-subsystem-2.jar            — Prebuilt JAR for other teams to import WMS classes
│
├── lib/
│   ├── scm-exception-handler-v3.jar   — Subsystem 17: exception handler (committed)
│   ├── scm-exception-viewer-gui.jar   — Subsystem 17: exception GUI viewer (committed)
│   ├── scm-gui.properties             — Exception viewer config: subsystem name (committed)
│   ├── database.properties            — GITIGNORED: MySQL credentials for direct JDBC
│   ├── database-module-*.jar          — GITIGNORED: Subsystem 15 DB module
│   ├── packing-subsystem-*.jar        — GITIGNORED: Subsystem 14 packing
│   ├── rfid-tracker-*.jar             — GITIGNORED: Subsystem 11 RFID
│   ├── jna-5.18.1.jar                 — GITIGNORED: JNA for Windows Event Viewer
│   ├── jna-platform-5.18.1.jar        — GITIGNORED: JNA platform for Windows Event Viewer
│   └── demand-forecasting-*.jar       — GITIGNORED: Subsystem 8 demand forecasting
│
├── .mvn/
│   └── jvm.config                     — GITIGNORED: JVM system properties for DB module
│
└── src/
    ├── Main.java                       — Composition root: wires all 12 boot steps in order
    │
    └── wms/
        │
        ├── commands/                   — Command pattern: queued warehouse task objects
        │   ├── IWarehouseTask.java     — Interface: single execute() method
        │   ├── CycleCountTask.java     — Bin audit task: scans and verifies stock counts
        │   └── InterleavedTask.java    — Combined pick+putaway task: reduces worker deadheading
        │
        ├── contracts/                  — Abstract contracts for external subsystem hooks
        │   ├── IWMSRepository.java     — DB contract: validatePurchaseOrder, recordStockMovement
        │   └── WarehouseSubsystemBase.java — Abstract base: reserveStockForOrder, processInboundScan
        │
        ├── controllers/                — GRASP Controllers: orchestrate multi-step flows
        │   ├── InboundReceivingController.java  — Full dock-to-shelf: ASN → GRN → QC → putaway
        │   └── OutboundTaskController.java      — Background poller: 4-poll loop, DLQ, exception escalation
        │
        ├── demo/                       — Standalone demonstration classes
        │   └── ExceptionDemo.java      — Triggers all 12 WMS exceptions; run independently
        │
        ├── exceptions/                 — WMS exception hierarchy
        │   ├── WMSException.java               — Base checked exception
        │   ├── WmsCoreException.java            — Base unchecked runtime exception
        │   ├── BinCapacityExceededException.java — Carries binId and limit
        │   └── InsufficientStockException.java  — Carries productId, requested, available
        │
        ├── factories/                  — Factory pattern: creates storage unit objects
        │   └── StorageUnitFactory.java — Creates Pallet, Tote, or Case by StorageUnitType enum
        │
        ├── integration/                — Anti-Corruption Layer: all external subsystem adapters
        │   │
        │   ├── — Interfaces (stable contracts for each integration) —
        │   ├── IWMSRepository.java              — Task polling and status update contract
        │   ├── IRFIDSystemAdapter.java           — RFID scan, tally, and verification contract
        │   ├── IWarehousePackingIntegration.java — Packing job lifecycle contract
        │   ├── IWarehousePackingStatusAdapter.java — Extended packing status contract
        │   ├── ITMSIntegrationService.java       — TMS event polling contract
        │   ├── IColdChainVerificationService.java — Temperature monitoring contract
        │   ├── ICrossDockingService.java          — Cross-dock routing contract
        │   ├── IDispatchGateway.java              — Exit gate scan contract
        │   ├── IDemandForecastingIntegration.java — Forecast query contract
        │   ├── IPackingVerificationService.java   — Packing confirmation contract
        │   ├── IRFIDAuditorService.java            — RFID audit contract
        │   ├── IReturnsManagementService.java      — Returns disposition contract
        │   └── IYardManagementService.java         — Yard and dock management contract
        │
        │   ├── — Subsystem Adapters —
        │   ├── RFIDSystemAdapter.java            — Subsystem 11: reflection-based dual live/stub
        │   ├── Subsystem14PackingAdapter.java    — Subsystem 14: creates/updates packing jobs
        │   ├── WarehouseManagementDatabaseAdapter.java — Subsystem 15: WMS tables via adapter
        │   ├── SafeExceptionAdapter.java         — Subsystem 17: routes all 12 WMS exceptions
        │   ├── Subsystem8ForecastAdapter.java    — Subsystem 8: circuit-breaker forecast query
        │   ├── TMSIntegrationService.java        — Subsystem 6: singleton shipment event store
        │   └── ShipmentReadyEvent.java           — Event model: READY_FOR_PICKUP / CANNOT_SHIP
        │
        │   ├── — Repository Layer —
        │   ├── SCMDatabaseAdapter.java           — Primary repository: simulates task fetch/update
        │   ├── InMemoryWMSRepository.java        — Fallback repository: in-memory task store
        │   └── ResilientRepositoryProxy.java     — Circuit breaker: primary + fallback
        │
        │   ├── — Database Layer —
        │   ├── DatabaseConnectionManager.java    — Singleton JDBC connection from database.properties
        │   └── DatabaseSeeder.java               — Reads 14 Excel sheets, seeds 12 MySQL tables
        │
        │   └── — Operational Services —
        │       ├── ColdChainVerificationService.java — Temperature QC, fires DAMAGED_GOODS event
        │       ├── CrossDockingService.java          — Bypasses putaway for urgent backorders
        │       ├── DispatchGateway.java               — Gate scanner, publishes TMS events
        │       ├── PackingVerificationService.java    — Confirms packing before dispatch
        │       ├── RFIDAuditorService.java            — RFID bin audit, fires CYCLE_COUNT event
        │       ├── ReturnsManagementService.java      — Return condition validation and routing
        │       └── YardManagementService.java         — Dock assignment, fires DOCK_DOUBLE_BOOKING
        │
        ├── models/                     — Domain model classes (pure data, no DB logic)
        │   ├── Product.java            — SKU, name, category (DRY_GOODS/PERISHABLE_COLD/HIGH_VALUE)
        │   ├── ProductCategory.java    — Enum for product storage classification
        │   ├── Order.java              — Outbound order with line items map
        │   ├── WarehouseTask.java      — Task with ID, product, bin, status, type
        │   ├── GRN.java                — Goods Receipt Note: header with item map
        │   ├── GRNItem.java            — GRN line: productId, receivedQty, damagedQty, acceptedQty
        │   ├── PurchaseOrder.java      — PO with supplier, warehouse, dates, status
        │   ├── POItem.java             — PO line item: product, qty, price
        │   ├── AdvanceShipmentNotice.java — ASN linking incoming delivery to PO
        │   ├── ASNItem.java            — ASN line item
        │   ├── Supplier.java           — Supplier with lead time and reliability score
        │   ├── SupplierInvoice.java    — Invoice header for 3-way matching
        │   ├── InvoiceItem.java        — Invoice line item
        │   ├── QualityInspection.java  — QC record: passed/failed quantities
        │   ├── Discrepancy.java        — Procurement discrepancy: type, product, supplier
        │   ├── WarehouseParameters.java — Config for Demand Forecasting: lead time, capacity etc.
        │   ├── ReplenishmentOrder.java  — Replenishment PO pushed by Demand Forecasting
        │   ├── StorageUnit.java         — Abstract storage unit base class
        │   ├── StorageUnitType.java     — Enum: PALLET, TOTE, CASE
        │   ├── Pallet.java             — Pallet storage unit
        │   ├── Tote.java               — Tote storage unit
        │   └── Case.java               — Case storage unit
        │
        ├── observers/                  — Observer pattern: inventory event listeners
        │   ├── IInventoryObserver.java         — Interface: onStockBelowThreshold callback
        │   └── ReplenishmentService.java       — Fires replenishment alert when stock is low
        │
        ├── services/                   — Core WMS business logic
        │   ├── WarehouseFacade.java     — MAIN ENTRY POINT: coordinates all WMS operations
        │   ├── InventoryManager.java    — Stock ledger: add, reserve, observer notification
        │   ├── OrderPickingEngine.java  — Executes picking using configured strategy
        │   ├── TaskEngine.java          — Executes queued IWarehouseTask commands
        │   ├── SlottingOptimizerService.java — Optimizes bin assignment using forecast signals
        │   ├── ProcurementService.java  — PO lifecycle management within WMS
        │   ├── VendorSelectionEngine.java — Selects optimal vendor by lead time and reliability
        │   └── WMSLogger.java           — Internal error logging utility
        │
        ├── strategies/                 — Strategy pattern: interchangeable algorithms
        │   ├── IPickingStrategy.java    — Interface: generatePickList + optimizePickPath
        │   ├── WavePickingStrategy.java — Sorts tasks by bin ID for optimized routing
        │   ├── ZonePickingStrategy.java — Assigns tasks by warehouse zone
        │   ├── IPutawayStrategy.java    — Interface: determineStorageBin
        │   ├── ColdChainStrategy.java   — Routes perishables to cold storage bins
        │   └── StandardFIFOStrategy.java — Routes dry goods using first-in-first-out logic
        │
        └── views/
            └── WarehouseTerminalView.java — Console output: printSystemEvent, printWarning, printError
```

---

## Key Commands Summary

```cmd
# Full setup from scratch
mvn clean compile

# Run the main WMS application
mvn exec:java -Dexec.mainClass="Main"

# Run the exception handling demo (all 12 exceptions)
mvn exec:java -Dexec.mainClass="wms.demo.ExceptionDemo"

# Launch Exception Viewer GUI (run from lib/ directory)
cd lib
java -cp ".;scm-exception-viewer-gui.jar;scm-exception-handler-v3.jar;jna-5.18.1.jar;jna-platform-5.18.1.jar" com.scm.gui.ExceptionViewerGUI

# Rebuild integration JAR
mvn clean package -DskipTests
copy target\warehouse-management-1.0-SNAPSHOT.jar dist\wms-subsystem-2.jar

# Register Windows Event Viewer (run as Administrator, one time only)
reg add "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\EventLog\Application\SCM-WarehouseMgmt" /v EventMessageFile /t REG_SZ /d "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\EventLogMessages.dll" /f
```

---

## Integration Guide

For complete details on integrating with WMS — including API contracts, method signatures, event models, boot output verification, and rules for each of the 6 subsystems — see **[INTEGRATION.md](INTEGRATION.md)**.

A prebuilt JAR is available at `dist/wms-subsystem-2.jar` for teams that want to import WMS classes directly.

---

## Notes

- The `procurement_final_dataset.xlsx` file must be in the project root directory when running the application. The `DatabaseSeeder` reads it at boot and will skip seeding if the file is not found.
- All gitignored files (`lib/database.properties`, `.mvn/jvm.config`, large JARs) must be created or obtained manually before the first run. The application will degrade gracefully if any external subsystem JAR is missing — it will not crash.
- The `target/` directory is generated by Maven and must never be committed. It is excluded by `.gitignore`.
