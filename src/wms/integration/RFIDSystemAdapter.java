package wms.integration;

import wms.models.Product;
import wms.models.ProductCategory;
import wms.services.WMSLogger;
import wms.views.WarehouseTerminalView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete Adapter connecting WMS to Subsystem 11 (Barcode Reader and RFID Tracker).
 *
 * DESIGN PATTERN — Adapter (Object Adapter):
 * This is the ONLY file in WMS that imports Subsystem 11 classes.
 * Currently operates as a self-contained stub until Subsystem 11 provides their JAR.
 * When their JAR is available, only this file needs updating.
 *
 * SOLID — Single Responsibility: handles only the translation between
 * Subsystem 11's scan data and WMS domain objects.
 *
 * GRASP — Indirection: sits between WarehouseFacade and Subsystem 11,
 * keeping both sides decoupled from each other.
 *
 * EXCEPTION POLICY: logs errors via WMSLogger, takes no corrective action.
 */
public class RFIDSystemAdapter implements IRFIDSystemAdapter {

    // Per-dock scan tally buffer. Key: dockId, Value: (SKU -> scan count).
    // Populated by processTagScan(), consumed and cleared by getDockScanTally().
    private final Map<String, Map<String, Integer>> dockTallyBuffer = new HashMap<>();

    // Session log of recent scan events per dock for reporting purposes.
    // Key: dockId, Value: list of "EVT-ID:TAG:STATUS" strings.
    private final Map<String, List<String>> recentScansLog = new HashMap<>();

    // Counter for generating unique scan event IDs in stub mode.
    private int eventCounter = 1;

    /**
     * NOTE FOR SUBSYSTEM 11 INTEGRATION:
     * When your JAR is available, inject RFIDSystemFacade here via constructor.
     * Replace the stub logic in processTagScan() with a call to:
     *     rfidFacade.submitScan(rfidTag, "RFID")
     * Then map the returned ScanResult to a WMS Product using mapToWmsProduct().
     * Do NOT call AddStockStrategy inside submitScan() — inventory is WMS's responsibility.
     */
    public RFIDSystemAdapter() {
        WarehouseTerminalView.printSystemEvent("RFIDSystemAdapter",
                "Initialised in STUB mode. Awaiting Subsystem 11 JAR for live scanning.");
    }

    /**
     * Processes a single RFID/barcode tag scan at a dock.
     * Identifies the product, tallies it in the dock buffer, and logs the event.
     * Returns null if the tag is unrecognised — callers must handle null gracefully.
     */
    @Override
    public Product processTagScan(String rfidTag, String dockId) {
        try {
            // STUB: Derive a Product from the tag string for simulation.
            // REPLACE THIS BLOCK when Subsystem 11 JAR is available.
            Product product = resolveProductFromTag(rfidTag);

            if (product == null) {
                WarehouseTerminalView.printWarning("RFIDSystemAdapter",
                        "Unknown tag scanned at " + dockId + ": " + rfidTag);
                WMSLogger.logError("RFIDSystemAdapter.processTagScan",
                        "Unresolved RFID tag: " + rfidTag + " at dock: " + dockId);
                return null;
            }

            // Accumulate scan count in the dock tally buffer.
            dockTallyBuffer
                    .computeIfAbsent(dockId, k -> new HashMap<>())
                    .merge(product.getSku(), 1, Integer::sum);

            // Log the scan event for reporting.
            String eventId = String.format("EVT-%03d", eventCounter++);
            recentScansLog
                    .computeIfAbsent(dockId, k -> new ArrayList<>())
                    .add(0, eventId + ":" + rfidTag + ":OK");

            WarehouseTerminalView.printSystemEvent("RFIDSystemAdapter",
                    "Scan OK | Tag: " + rfidTag + " -> SKU: " + product.getSku()
                            + " | Dock: " + dockId + " | Event: " + eventId);

            return product;

        } catch (Exception e) {
            WMSLogger.logError("RFIDSystemAdapter.processTagScan", e.getMessage());
            return null;
        }
    }

    /**
     * Returns the accumulated SKU scan counts for a dock and clears the buffer.
     * Cleared immediately after retrieval to prevent double-counting on the next arrival.
     */
    @Override
    public Map<String, Integer> getDockScanTally(String dockId) {
        Map<String, Integer> tally = dockTallyBuffer.getOrDefault(dockId, Collections.emptyMap());
        dockTallyBuffer.remove(dockId);
        WarehouseTerminalView.printSystemEvent("RFIDSystemAdapter",
                "Dock tally retrieved and cleared for " + dockId + " | Items: " + tally);
        return Collections.unmodifiableMap(tally);
    }

    /**
     * Verifies that scanned outbound RFID tags are non-empty.
     * Full order-level tag matching will be implemented when Subsystem 11 JAR is available.
     */
    @Override
    public boolean verifyOutboundTags(String orderId, List<String> rfidTags) {
        if (rfidTags == null || rfidTags.isEmpty()) {
            WarehouseTerminalView.printWarning("RFIDSystemAdapter",
                    "Outbound tag verification FAILED for Order " + orderId + " — no tags scanned.");
            return false;
        }
        WarehouseTerminalView.printSystemEvent("RFIDSystemAdapter",
                "Outbound tag verification PASSED for Order " + orderId
                        + " | Tags scanned: " + rfidTags.size());
        return true;
    }

    /**
     * Returns recent scan log entries for a dock, up to the requested limit.
     * Most recent entries are returned first.
     */
    @Override
    public List<String> getRecentDockScans(String dockId, int limit) {
        List<String> logs = recentScansLog.getOrDefault(dockId, Collections.emptyList());
        return Collections.unmodifiableList(logs.subList(0, Math.min(limit, logs.size())));
    }

    /**
     * STUB — maps an RFID tag string to a WMS Product for simulation purposes.
     * Uses tag content keywords to infer product category.
     *
     * REPLACE THIS METHOD when Subsystem 11 JAR is available — the real
     * implementation will call DatabaseManager.findProductByRfidTag(tag)
     * from their codebase and then translate the result using mapToWmsProduct().
     *
     * @param rfidTag  Raw tag string from the scanner
     * @return         A simulated Product, or null if tag is blank or null
     */
    private Product resolveProductFromTag(String rfidTag) {
        if (rfidTag == null || rfidTag.isBlank()) {
            return null;
        }

        // Infer category from tag keyword for stub simulation.
        String tagUpper = rfidTag.toUpperCase();
        ProductCategory category;
        if (tagUpper.contains("DAIRY") || tagUpper.contains("COLD")
                || tagUpper.contains("FRESH") || tagUpper.contains("PHARMA")) {
            category = ProductCategory.PERISHABLE_COLD;
        } else if (tagUpper.contains("HIGHVAL") || tagUpper.contains("ELEC")
                || tagUpper.contains("JEWEL")) {
            category = ProductCategory.HIGH_VALUE;
        } else {
            category = ProductCategory.DRY_GOODS;
        }

        // Derive a stable SKU from the tag string.
        String sku = "SKU-" + rfidTag.replaceAll("[^A-Za-z0-9]", "-").toUpperCase();
        return new Product(sku, "Product[" + rfidTag + "]", category);
    }
}
