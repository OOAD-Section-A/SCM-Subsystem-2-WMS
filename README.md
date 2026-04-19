# Warehouse Management System (WMS) - Subsystem 2

---

## 🏗️ Architecture & Integration Points

To ensure loose coupling and high cohesion, the WMS exposes its functionality to partner teams via specific **Integration Services** and a central **Facade**.

### 🔗 How Other Teams Can Integrate With Us
1. **RFID System (Team NOVA):** 
   - WMS consumes your tag scans via `WarehouseFacade.processInboundScan()`. You act as the scanner; we handle the math, the Quality Control (QC), and the official Goods Receipt Note (GRN) generation.
2. **Real-Time Delivery Monitoring (Team Ramen Noodles):** 
   - We listen to your `GEOFENCE_ENTRY` events via our `YardManagementService` to automatically assign dock doors for arriving trucks.
   - We verify your `TEMPERATURE_THRESHOLD_BREACH` alerts against our RFID dock scans via `ColdChainVerificationService` to reject spoiled goods.
3. **Order Fulfillment (Team VERTEX):**
   - We use our `PackingVerificationService` to cross-reference outbound physical RFID scans against your digital `Order` objects to guarantee 100% packing accuracy.

> **Note on Exceptions:** The WMS is fully wired into the central **SCM Exception Handler** (`WarehouseMgmtSubsystem.INSTANCE`). We automatically log discrepancies like `DOCK_DBL_BOOKING`, `INSUFFICIENT_STOCK_FOR_PICK`, and `GRN_QTY_MISMATCH`.

---

## 📁 Directory Structure & Key Files

```text
SCM-Subsystem-2-WMS/
├── src/
│   ├── Main.java                        # The master simulation class
│   └── wms/
│       ├── commands/                    # [Command Pattern] Warehouse worker tasks
│       ├── contracts/                   # Interfaces defining broad subsystem contracts
│       ├── controllers/                 # [GRASP Controller] Workflow orchestrators
│       ├── exceptions/                  # Local WMS custom exceptions
│       ├── factories/                   # [Factory Pattern] Object creation
│       ├── models/                      # Domain Entities (Product, GRN, PurchaseOrder, etc.)
│       ├── observers/                   # [Observer Pattern] Event listeners (e.g., Low Stock)
│       ├── services/                    # Core Business Logic & Engines
│       │   └── integration/             # Isolated Gateway/Adapter classes for partner teams
│       └── strategies/                  # [Strategy Pattern] Interchangeable algorithms (Putaway, Picking)
```

### 🌟 The Most Important Java Files

If you are exploring the codebase, start here:

#### 1. `WarehouseFacade.java` (in `services/`)
* **Design Pattern:** Facade
* **Role:** The primary "front door" to the WMS. Other subsystems should rarely bypass the Facade. It orchestrates receiving, putaway, picking, and task delegation without exposing the complex internal engines.

#### 2. `InventoryManager.java` (in `services/`)
* **Design Pattern:** Information Expert & Observer (Subject)
* **Role:** Holds the official `stockLedger`. When stock dips below the safety threshold, it fires an event to its Observers (like the `ReplenishmentService`) to automatically trigger a new Purchase Order. It also throws the `INSUFFICIENT_STOCK_FOR_PICK` exception centrally if fulfillment requests exceed capacity.

#### 3. `InboundReceivingController.java` (in `controllers/`)
* **Design Pattern:** GRASP Controller
* **Role:** Manages the chaotic dock doors. It registers incoming Advance Shipment Notices (ASNs), tallies RFID scans, executes Quality Control (QC) inspections, generates the GRN, and routes damaged goods appropriately.

#### 4. `VendorSelectionEngine.java` & `ProcurementService.java` (in `services/`)
* **Role:** The WMS doesn't just store boxes; it manages procurement. The `VendorSelectionEngine` evaluates suppliers using a weighted multi-variable formula (Quality, Delivery Speed, Price, Service). The `ProcurementService` executes the strict "3-Way Match" (Purchase Order == GRN == Supplier Invoice) to authorize or block payments.

#### 5. `wms.services.integration` (The Integration Package)
To protect our core logic from external changes, all cross-team interactions are isolated here:
* `YardManagementService`: Allocates dock doors upon truck Geofence entry.
* `ColdChainVerificationService`: Protects perishable goods by checking RFID temps against transit temps.
* `PackingVerificationService`: Validates outbound boxes.
* `DispatchGateway`: Simulates the Gate Pass for outbound trucks, handing off to Real-Time Delivery.
* `RFIDAuditorService`: Allows workers to do bin cycle-counts using mobile RFID scanners.

---

## 🚀 Running the Simulation

You can watch the entire WMS lifecycle (Receiving, Task Execution, 3-Way Matching, Vendor Selection, and External Integrations) play out in the console by running:

```bash
# Compile the project
javac -d out $(find src -name "*.java")

# Run the master simulation
java -cp out Main
```

*(Note: To test the Exception Handler popups, you must add the Java 17 compiled `scm-exception-handler-v3.jar` to your classpath).*
