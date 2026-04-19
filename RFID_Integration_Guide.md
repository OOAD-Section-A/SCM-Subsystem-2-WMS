# RFID System Integration Guide

This document explains in detail how the Warehouse Management System (WMS) utilizes the RFID Subsystem (Team NOVA) across various logical operations.

## 1. Architectural Overview

The RFID Subsystem acts as a "dumb scanner" (Information Expert) that reads physical tags and logs events. The WMS acts as the central intelligence (Controller) that consumes these events and turns them into meaningful supply chain operations. 

WMS integrates with RFID across **both Inbound and Outbound** workflows.

## 2. Integration Workflows

### A. Inbound Receiving (GRN Generation)
* **Flow:** As an inbound truck is unloaded, workers scan the incoming pallets/cartons using RFID. 
* **WMS Action:** The `WarehouseFacade.processInboundScan()` method tallies these individual scans in memory. Once unloading is complete, the `InboundReceivingController` verifies the aggregate count against the Advance Shipment Notice (ASN) and generates a bulk Goods Receipt Note (GRN).
* **Benefit:** Fixes the original bug where the RFID system was directly making 500 individual stock additions for a 500-item shipment, which violated inventory rules.

### B. Outbound Packing (Verification)
* **Flow:** During order fulfillment, a worker places picked items into an outbound shipping box and scans their tags.
* **WMS Action:** The `PackingVerificationService` receives the digital `Order` and the list of physical RFID scans. It strictly verifies that the scanned items perfectly match the order's line items. If successful, the WMS triggers the `PackagingSystemAdapter` to hand the order off to the external Packaging System.

### C. Cycle Counting (Inventory Auditing)
* **Flow:** A warehouse auditor sweeps a storage bin (e.g., Aisle 4, Rack B) with a mobile RFID scanner.
* **WMS Action:** The `RFIDAuditorService` (injected into `CycleCountTask`) compares the physical tags detected against the expected stock levels in the WMS Database. Discrepancies trigger a `CYCLE_COUNT_DISCREPANCY` exception to the central handler.

### D. Outbound Dispatch & Gate Pass
* **Flow:** A fully loaded delivery truck drives through the warehouse exit gate, passing a long-range RFID scanner.
* **WMS Action:** The `DispatchGateway` verifies that the items inside the truck perfectly match the `DeliveryOrder`, preventing theft or misloaded pallets before the truck enters the yard.

### E. Cold Chain Verification (Logistics)
* **Flow:** WMS cross-references real-time dock RFID temperature sensors against transit data.
* **WMS Action:** `ColdChainVerificationService` checks if perishable goods (e.g., Dairy, Meat) breached safe temperatures (e.g., > 4.0°C) during unloading, triggering a rejection.

## 3. File Structure

```text
SCM-Subsystem-2-WMS/
└── src/
    └── wms/
        ├── controllers/
        │   └── InboundReceivingController.java     # Consumes RFID for GRNs
        └── services/
            ├── WarehouseFacade.java                # Aggregates Inbound RFID scans
            └── integration/
                ├── PackingVerificationService.java # Verifies Outbound Packing RFID scans
                ├── RFIDAuditorService.java         # Automates Cycle Counting via RFID
                ├── DispatchGateway.java            # Validates Truck Gate Pass via RFID
                ├── ReturnsManagementService.java   # Re-routes rejected RFID packages
                └── ColdChainVerificationService.java # Monitors RFID Temperature sensors
```
