# Packaging System Integration Guide

## 1. Architectural Overview

The WMS acts as the central orchestrator bridging the physical warehouse hardware (RFID Scanners) with the digital fulfillment systems.We are using the **Adapter Pattern** to strictly decouple the WMS from external UI and logic constraints.

**Important Note:** The Packaging Subsystem is utilized strictly for **Outbound** operations (boxing items to ship). Inbound receiving operations (de-palletizing/unpacking) are handled natively by WMS Putaway processes without requiring the external packaging module.

## 2. The Integration Workflow (Outbound Packing)

1. **Order Initiation:** The Order Fulfillment Subsystem drops a digital `Order` into the WMS.
2. **Physical Execution:** A warehouse worker physically places the items into a shipping box and scans their RFID tags.
3. **RFID Verification:** The `PackingVerificationService` takes the digital `Order` and the list of physical RFID scans. It mathematically guarantees a 100% match, preventing mis-shipments.
4. **Metadata Encoding:** If the verification succeeds, the WMS prepares a `PackingRequest`. Because the external Packaging API only accepts four strict fields (`itemId`, `description`, `weightKg`, `fragile`), WMS safely encodes critical product metadata (Name, Product Type/Perishability, and Delivery Date) directly into the `description` string.
5. **System Handoff:** The `PackagingSystemAdapter` dispatches this enriched payload to the external `IWarehousePackingIntegration` interface, triggering their external logic and UI.

## 3. File Structure

The integration is encapsulated within the `wms.services.integration` package to isolate external dependencies from core WMS business logic.

```text
SCM-Subsystem-2-WMS/
└── src/
    └── wms/
        └── services/
            └── integration/
                ├── IExternalPackingService.java        # Interface defining the delegation contract
                ├── PackagingSystemAdapter.java         # The Adapter connecting WMS to the external JAR
                ├── IPackingVerificationService.java    # Interface for RFID verification
                └── PackingVerificationService.java     # Implements the verification logic
```

### Key Classes
* `PackagingSystemAdapter`: Implements `IExternalPackingService`. It is responsible for translating a WMS `Order` and a Map of `Product` objects into the external `PackingRequest` object, handling the mapping of fragility to booleans and encoding perishability into the description.
* `PackingVerificationService`: Implements `IPackingVerificationService`. It acts as the gateway between the RFID scanners and the Packaging System Adapter.
