package wms.integration;

import wms.models.Product;
import java.util.List;
import java.util.Map;

/**
 * Anti-Corruption Layer interface between Subsystem 2 (Warehouse Management)
 * and Subsystem 11 (Barcode Reader and RFID Tracker).
 *
 * DESIGN PATTERN — Adapter (Target Interface):
 * This interface is the "target" side of the Adapter pattern. WMS defines
 * what it needs from RFID scanning in its own terms. The concrete class
 * RFIDSystemAdapter (which implements this interface) is the only place in
 * the entire WMS codebase that will ever import or reference Subsystem 11
 * classes directly. If Subsystem 11 changes their internal API, only
 * RFIDSystemAdapter needs updating — nothing else in WMS changes.
 *
 * SOLID — Dependency Inversion Principle (D):
 * WarehouseFacade and InboundReceivingController depend on this interface,
 * not on any concrete RFID class. This means WMS can compile, run, and be
 * tested entirely independently of whether Subsystem 11's JAR is available.
 *
 * GRASP — Protected Variations:
 * This interface protects WMS from variations in Subsystem 11's implementation.
 * If Subsystem 11 switches from RFID to barcode-only or adds new scan modes,
 * WMS is completely shielded from those changes.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 11 (Barcode Reader and RFID Tracker):
 * To integrate with Subsystem 2 (WMS), your team does not call WMS directly.
 * WMS calls your RFIDSystemFacade.submitScan() through our RFIDSystemAdapter.
 * The only requirement from your side is that submitScan() does NOT modify
 * inventory directly — inventory updates are exclusively WMS's responsibility.
 * See RFIDSystemAdapter.java for the exact methods we call on your facade.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 1 (Inventory Management):
 * Stock levels are updated by WMS after RFID scan reconciliation is complete,
 * not during the scan itself. This prevents double-counting.
 */
public interface IRFIDSystemAdapter {

    /**
     * Submits a single RFID or barcode tag scan for a product arriving at a dock.
     *
     * Called repeatedly during inbound receiving as each item is unloaded from
     * a supplier truck. Each call identifies the product from the tag and
     * accumulates the count in an internal per-dock tally buffer.
     *
     * This method does NOT modify inventory. It only records that a physical
     * item was scanned at a specific dock. Inventory is updated later by
     * InboundReceivingController after the full GRN is created.
     *
     * @param rfidTag  The raw tag string read by the scanner hardware
     *                 (e.g., "RFID-TAG-001" or a barcode string)
     * @param dockId   The dock door identifier where scanning is occurring
     *                 (e.g., "Dock-A", "Dock-B"). Used to group scans by arrival.
     * @return         The WMS Product object identified by this tag,
     *                 or null if the tag is unknown or the scan fails.
     *                 Callers must handle the null case gracefully.
     */
    Product processTagScan(String rfidTag, String dockId);

    /**
     * Returns the accumulated scan tally for a specific dock and then clears it.
     *
     * Called by InboundReceivingController after all items from a truck have
     * been scanned. The returned map is used to reconcile physical scan counts
     * against the expected quantities in the Advance Shipment Notice (ASN).
     *
     * The tally is cleared atomically after retrieval to prevent double-counting
     * on the next truck arrival at the same dock.
     *
     * @param dockId  The dock door identifier to query (e.g., "Dock-A")
     * @return        A map of SKU to scan count for that dock.
     *                Example: {"SKU-DAIRY-1": 50, "SKU-BREAD-3": 20}
     *                Returns an empty map (never null) if no scans are recorded.
     */
    Map<String, Integer> getDockScanTally(String dockId);

    /**
     * Verifies that a set of RFID tags scanned during outbound packing
     * matches the expected items in an outbound order.
     *
     * Called by PackingVerificationService before handing off a packing job
     * to Subsystem 14 (Packaging, Repairs, Receipt Management). Acts as a
     * physical reality check — confirms the right items are actually on the
     * packing bench before the digital dispatch happens.
     *
     * @param orderId   The WMS outbound order ID being verified
     * @param rfidTags  List of RFID tag strings scanned at the packing station
     * @return          true if all order items have matching scanned tags,
     *                  false if there is any mismatch (missing or extra items)
     */
    boolean verifyOutboundTags(String orderId, List<String> rfidTags);

    /**
     * Retrieves recent scan records for a specific dock for display or audit.
     *
     * Used by Subsystem 5 (Reporting and Analytics Dashboard) and by
     * WarehouseTerminalView for real-time dock activity display.
     *
     * @param dockId  The dock door to query
     * @param limit   Maximum number of recent scan records to return
     * @return        List of scan event strings, most recent first.
     *                Format: "EVT-ID:TAG:STATUS" (e.g., "EVT-001:RFID-001:OK")
     *                Returns an empty list (never null) if no records exist.
     */
    List<String> getRecentDockScans(String dockId, int limit);
}