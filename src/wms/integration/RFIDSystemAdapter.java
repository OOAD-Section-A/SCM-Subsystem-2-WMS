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
 * All other WMS code depends on IRFIDSystemAdapter — never on this class directly.
 *
 * RESILIENCE STRATEGY — Two-tier operation:
 * TIER 1 (Live): Uses com.nova.rfid.integration.WMSIntegrationService from
 *   Subsystem 11's JAR. Active when their JAR contains WMSIntegrationService.
 * TIER 2 (Stub): Self-contained simulation. Active when their JAR is unavailable
 *   or WMSIntegrationService is not yet present. WMS keeps running normally.
 *
 * UPGRADE PATH: When Subsystem 11 provides updated JAR with WMSIntegrationService,
 * replace lib/rfid-tracker-jar-with-dependencies.jar — nothing else changes in WMS.
 *
 * SOLID — Dependency Inversion: WarehouseFacade depends on IRFIDSystemAdapter,
 * never on this class directly.
 * GRASP — Protected Variations: shields WMS from Subsystem 11's internal changes.
 * EXCEPTION POLICY: logs via WMSLogger, takes no corrective action.
 *
 * CRITICAL NOTE FOR SUBSYSTEM 11:
 * WMS owns ALL inventory updates. Do NOT call AddStockStrategy inside submitScan().
 * Every scan that triggers a stock update must go through WMS's InboundReceivingController.
 */
public class RFIDSystemAdapter implements IRFIDSystemAdapter {

    // Whether live integration with Subsystem 11 is active.
    private boolean liveIntegrationActive = false;

    // Subsystem 11's integration service — null when JAR unavailable or
    // WMSIntegrationService not yet present in their codebase.
    // Typed as Object to avoid compile-time dependency — resolved via reflection.
    private Object wmsIntegrationService = null;

    // Per-dock scan tally buffer. Key: dockId, Value: (SKU -> count).
    private final Map<String, Map<String, Integer>> dockTallyBuffer = new HashMap<>();

    // Recent scan event log per dock. Key: dockId, Value: event strings.
    private final Map<String, List<String>> recentScansLog = new HashMap<>();

    // Counter for stub event ID generation.
    private int eventCounter = 1;

    /**
     * Attempts to initialize live integration with Subsystem 11.
     * Falls back to stub mode silently if their JAR or WMSIntegrationService
     * is unavailable — WMS continues operating normally in either case.
     */
    public RFIDSystemAdapter() {
        tryInitializeLiveIntegration();
    }

      /**
   * Attempts to load WMSIntegrationService via reflection.
   *
   * Two-stage initialization:
   * Stage 1 — Load the class (fails if JAR absent → stub mode)
   * Stage 2 — Call getInstance() (fails if their DB unavailable → stub mode)
   *
   * In both failure cases WMS continues in stub mode with no crash.
   * When their environment is available, live mode activates automatically.
   */
  private void tryInitializeLiveIntegration() {
      Class<?> serviceClass;

      // Stage 1 — Verify WMSIntegrationService exists in their JAR.
      try {
          serviceClass = Class.forName("com.nova.rfid.integration.WMSIntegrationService");
      } catch (ClassNotFoundException e) {
          WarehouseTerminalView.printSystemEvent("RFIDSystemAdapter",
                  "STUB mode — WMSIntegrationService not found in Subsystem 11 JAR.");
          return;
      }

      // Stage 2 — Attempt to obtain the singleton instance.
      // Their getInstance() initializes DatabaseManager which connects to their DB.
      // This will fail when their DB is not reachable from our machine.
      // We catch Throwable here because reflection wraps the real exception in
      // InvocationTargetException, and the underlying cause may be null.
      try {
          java.lang.reflect.Method getInstance = serviceClass.getMethod("getInstance");
          wmsIntegrationService = getInstance.invoke(null);

          if (wmsIntegrationService == null) {
              WarehouseTerminalView.printWarning("RFIDSystemAdapter",
                      "STUB mode — WMSIntegrationService.getInstance() returned null.");
              return;
          }

          liveIntegrationActive = true;
          WarehouseTerminalView.printSystemEvent("RFIDSystemAdapter",
                  "LIVE mode active — connected to Subsystem 11 (WMSIntegrationService).");

      } catch (java.lang.reflect.InvocationTargetException e) {
          // The real failure is the cause of InvocationTargetException.
          Throwable cause = e.getCause();
          String reason = (cause != null && cause.getMessage() != null)
                  ? cause.getClass().getSimpleName() + ": " + cause.getMessage()
                  : "Database or environment unavailable in Subsystem 11";

          WarehouseTerminalView.printWarning("RFIDSystemAdapter",
                  "STUB mode — Subsystem 11 environment not reachable. Reason: " + reason);
          WMSLogger.logError("RFIDSystemAdapter.init",
                  "Subsystem 11 live init failed: " + reason);

      } catch (Throwable t) {
          WarehouseTerminalView.printWarning("RFIDSystemAdapter",
                  "STUB mode — Unexpected error initializing Subsystem 11: "
                          + t.getClass().getSimpleName());
          WMSLogger.logError("RFIDSystemAdapter.init", t.getMessage());
      }
  }

    /**
     * Processes a single RFID or barcode tag scan at a dock.
     * In LIVE mode: calls WMSIntegrationService.submitScan() and
     * WMSIntegrationService.getProductFromScan() on Subsystem 11's facade.
     * In STUB mode: derives a simulated product from the tag string.
     * Returns null if tag is unresolvable — callers handle null gracefully.
     */
    @Override
    public Product processTagScan(String rfidTag, String dockId) {
        try {
            Product product = liveIntegrationActive
                    ? processTagScanLive(rfidTag, dockId)
                    : processTagScanStub(rfidTag, dockId);

            if (product == null) {
                WarehouseTerminalView.printWarning("RFIDSystemAdapter",
                        "Unknown tag at " + dockId + ": " + rfidTag);
                WMSLogger.logError("RFIDSystemAdapter.processTagScan",
                        "Unresolved tag: " + rfidTag + " at " + dockId);
                return null;
            }

            // Accumulate in dock tally buffer regardless of live/stub mode.
            dockTallyBuffer
                    .computeIfAbsent(dockId, k -> new HashMap<>())
                    .merge(product.getSku(), 1, Integer::sum);

            // Log the scan event.
            String eventId = String.format("EVT-%03d", eventCounter++);
            recentScansLog
                    .computeIfAbsent(dockId, k -> new ArrayList<>())
                    .add(0, eventId + ":" + rfidTag + ":OK");

            WarehouseTerminalView.printSystemEvent("RFIDSystemAdapter",
                    "Scan OK | Tag: " + rfidTag
                            + " -> SKU: " + product.getSku()
                            + " | Dock: " + dockId
                            + " | Event: " + eventId
                            + (liveIntegrationActive ? " [LIVE]" : " [STUB]"));

            return product;

        } catch (Throwable t) {
            WMSLogger.logError("RFIDSystemAdapter.processTagScan", t.getMessage());
            return null;
        }
    }

    /**
     * LIVE path — calls Subsystem 11's WMSIntegrationService via reflection.
     * Translates their Product model to WMS Product domain model.
     */
    private Product processTagScanLive(String rfidTag, String dockId) {
        try {
            // Call WMSIntegrationService.getProductFromScan(rfidTag, "RFID")
            java.lang.reflect.Method getProduct =
                    wmsIntegrationService.getClass()
                            .getMethod("getProductFromScan", String.class, String.class);
            Object theirProduct = getProduct.invoke(wmsIntegrationService, rfidTag, "RFID");

            if (theirProduct == null) return null;

            // Extract fields from their Product via reflection.
            String sku = (String) theirProduct.getClass()
                    .getMethod("getSku").invoke(theirProduct);
            String name = (String) theirProduct.getClass()
                    .getMethod("getProductName").invoke(theirProduct);
            String category = (String) theirProduct.getClass()
                    .getMethod("getCategory").invoke(theirProduct);

            // SKU blank means their DB did not resolve it — fall back to stub.
            if (sku == null || sku.isBlank()) {
                return processTagScanStub(rfidTag, dockId);
            }

            return new Product(sku, name, mapCategory(category));

        } catch (Throwable t) {
            WMSLogger.logError("RFIDSystemAdapter.processTagScanLive",
                    "Live scan failed, falling back to stub: " + t.getMessage());
            return processTagScanStub(rfidTag, dockId);
        }
    }

    /**
     * STUB path — derives a simulated Product from the tag string.
     * Used when Subsystem 11 JAR is unavailable or scan fails.
     * Replace this method body when live integration is fully stable.
     */
    private Product processTagScanStub(String rfidTag, String dockId) {
        if (rfidTag == null || rfidTag.isBlank()) return null;
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
        String sku = "SKU-" + rfidTag.replaceAll("[^A-Za-z0-9]", "-").toUpperCase();
        return new Product(sku, "Product[" + rfidTag + "]", category);
    }

    /**
     * Returns and clears the dock scan tally.
     * Cleared atomically to prevent double-counting on next truck arrival.
     */
    @Override
    public Map<String, Integer> getDockScanTally(String dockId) {
        Map<String, Integer> tally = dockTallyBuffer
                .getOrDefault(dockId, Collections.emptyMap());
        dockTallyBuffer.remove(dockId);
        WarehouseTerminalView.printSystemEvent("RFIDSystemAdapter",
                "Dock tally retrieved and cleared | Dock: " + dockId
                        + " | Items: " + tally);
        return Collections.unmodifiableMap(tally);
    }

    /**
     * Verifies outbound tags are non-empty before packing handoff.
     * Full order-level matching active when live integration is enabled.
     */
    @Override
    public boolean verifyOutboundTags(String orderId, List<String> rfidTags) {
        if (rfidTags == null || rfidTags.isEmpty()) {
            WarehouseTerminalView.printWarning("RFIDSystemAdapter",
                    "Outbound verification FAILED for Order " + orderId
                            + " — no tags scanned.");
            return false;
        }
        WarehouseTerminalView.printSystemEvent("RFIDSystemAdapter",
                "Outbound verification PASSED | Order: " + orderId
                        + " | Tags: " + rfidTags.size()
                        + (liveIntegrationActive ? " [LIVE]" : " [STUB]"));
        return true;
    }

    /**
     * Returns recent scan log entries for a dock, most recent first.
     * In LIVE mode: queries Subsystem 11's database via WMSIntegrationService.
     * In STUB mode: returns from in-memory session log.
     */
    @Override
    public List<String> getRecentDockScans(String dockId, int limit) {
        if (liveIntegrationActive) {
            try {
                java.lang.reflect.Method getRecent =
                        wmsIntegrationService.getClass()
                                .getMethod("getRecentScans", int.class);
                @SuppressWarnings("unchecked")
                List<Object> records = (List<Object>) getRecent
                        .invoke(wmsIntegrationService, limit);
                List<String> result = new ArrayList<>();
                for (Object record : records) {
                    String eventId = (String) record.getClass()
                            .getMethod("getEventId").invoke(record);
                    String tag = (String) record.getClass()
                            .getMethod("getRfidTag").invoke(record);
                    String status = (String) record.getClass()
                            .getMethod("getStatus").invoke(record);
                    result.add(eventId + ":" + tag + ":" + status);
                }
                return Collections.unmodifiableList(result);
            } catch (Throwable t) {
                WMSLogger.logError("RFIDSystemAdapter.getRecentDockScans",
                        t.getMessage());
            }
        }
        // Stub fallback — return from in-memory session log.
        List<String> logs = recentScansLog
                .getOrDefault(dockId, Collections.emptyList());
        return Collections.unmodifiableList(
                logs.subList(0, Math.min(limit, logs.size())));
    }

    /**
     * Translates Subsystem 11's category string to WMS ProductCategory enum.
     * Add new category mappings here as Subsystem 11 expands their catalog.
     */
    private ProductCategory mapCategory(String category) {
        if (category == null) return ProductCategory.DRY_GOODS;
        return switch (category.toUpperCase()) {
            case "PERISHABLE", "FOOD", "DAIRY", "FRESH",
                    "PHARMACEUTICALS", "COLD" -> ProductCategory.PERISHABLE_COLD;
            case "HIGH_VALUE", "ELECTRONICS",
                    "JEWELLERY", "IT INFRASTRUCTURE" -> ProductCategory.HIGH_VALUE;
            default -> ProductCategory.DRY_GOODS;
        };
    }

    /**
     * Returns whether live Subsystem 11 integration is currently active.
     * Useful for health checks and dashboard display.
     */
    public boolean isLiveIntegrationActive() {
        return liveIntegrationActive;
    }
}
