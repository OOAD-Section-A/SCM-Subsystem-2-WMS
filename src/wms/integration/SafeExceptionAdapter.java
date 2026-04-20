package wms.integration;

import com.scm.subsystems.WarehouseMgmtSubsystem;
import wms.exceptions.BinCapacityExceededException;
import wms.exceptions.InsufficientStockException;
import wms.views.WarehouseTerminalView;

import javax.swing.SwingUtilities;

/**
 * Anti-Corruption Layer between WMS and Subsystem 17 (Exception Handling).
 *
 * SACRED ARCHITECTURE — do not modify the following:
 * - SwingUtilities.invokeLater() wrapping of all JAR calls (prevents Swing thread crashes)
 * - Throwable catch (not Exception) on all JAR calls (absorbs NoClassDefFoundError)
 * - useExternalJar circuit breaker flag
 *
 * DESIGN PATTERN — Adapter:
 * Translates WMS exception objects into Subsystem 17's typed method calls.
 * WMS exception classes never import Subsystem 17 classes directly.
 *
 * EXCEPTION POLICY — per faculty requirement:
 * All WMS exceptions are logged via Subsystem 17's API.
 * No corrective action is taken here. Exceptions appear in:
 *   1. A blocking popup on the machine
 *   2. Windows Event Viewer under SCM-WarehouseMgmt
 *   3. Subsystem 17's GUI
 *
 * SUBSYSTEM 17 EXCEPTION REFERENCE — WMS methods (from Exception_Method_Reference.docx):
 *   ID 13  — onInvalidPurchaseOrderReference(poReference)
 *   ID 14  — onInvalidProductReference(productId)
 *   ID 19  — onReturnConditionInvalid(returnId, conditionCode)
 *   ID 107 — onConcurrentUpdateConflict(productId)
 *   ID 152 — onInsufficientStockForPick(productId, requested, available)
 *   ID 153 — onStockUnderflow(productId, current, delta)
 *   ID 154 — onBinCapacityExceeded(binId, limit)
 *   ID 155 — onBinNotFoundOrBlocked(binId)
 *   ID 156 — onDockDoubleBooking(dockId, shipmentId1, shipmentId2)
 *   ID 313 — onCycleCountDiscrepancy(locationId, systemQty, physicalQty)
 *   ID 314 — onGrnQtyMismatch(grnId, poQty, receivedQty)
 *   ID 407 — onDamagedGoodsDetected(productId, binId)
 */
public class SafeExceptionAdapter {

    // Circuit breaker — flips to false if Subsystem 17 JAR is completely unavailable.
    private static boolean useExternalJar = true;

    /**
     * Routes a WMS exception to the correct Subsystem 17 typed method.
     * Called from OutboundTaskController DLQ path and anywhere else
     * a WMS exception needs to be formally reported.
     *
     * @param e  The caught WMS exception. Must not be null.
     */
    public static void handle(Exception e) {
        if (!useExternalJar) {
            localFallbackLog(e);
            return;
        }

        try {
            if (e instanceof BinCapacityExceededException bce) {
                // ID 154 — BIN_CAPACITY_EXCEEDED
                WarehouseTerminalView.printSystemEvent("SUBSYSTEM-17",
                        "Routing BIN_CAPACITY_EXCEEDED to Exception Handler...");
                SwingUtilities.invokeLater(() -> {
                    try {
                        WarehouseMgmtSubsystem.INSTANCE
                                .onBinCapacityExceeded(bce.getBinId(), (int) bce.getLimit());
                    } catch (Throwable t) { /* JAR unavailable — absorbed silently */ }
                });

            } else if (e instanceof InsufficientStockException ise) {
                // ID 152 — INSUFFICIENT_STOCK_FOR_PICK
                WarehouseTerminalView.printSystemEvent("SUBSYSTEM-17",
                        "Routing INSUFFICIENT_STOCK_FOR_PICK to Exception Handler...");
                SwingUtilities.invokeLater(() -> {
                    try {
                        WarehouseMgmtSubsystem.INSTANCE
                                .onInsufficientStockForPick(
                                        ise.getProductId(),
                                        ise.getRequested(),
                                        ise.getAvailable());
                    } catch (Throwable t) { /* JAR unavailable — absorbed silently */ }
                });

            } else {
                // Unregistered WMS exception — ID 0 placeholder per Exception Handler
                // integration guide Step 5. No JAR method exists for unregistered
                // exceptions, so we log locally only.
                WarehouseTerminalView.printWarning("SUBSYSTEM-17",
                        "Unregistered exception (ID 0): "
                                + e.getClass().getSimpleName()
                                + " — " + e.getMessage());
                localFallbackLog(e);
            }

        } catch (Throwable t) {
            // Outer fallback — JAR completely unavailable at startup.
            useExternalJar = false;
            WarehouseTerminalView.printWarning("SUBSYSTEM-17",
                    "Exception Handler offline. Switching to local fallback logging.");
            localFallbackLog(e);
        }
    }

    // ── DIRECT EXCEPTION ROUTING METHODS ─────────────────────────────────────
    // These methods are called directly from WMS service classes when a specific
    // exception condition is detected. Each maps to one Subsystem 17 method.
    // All are wrapped in SwingUtilities.invokeLater + Throwable catch.

    /** ID 13 — Called when a GRN references a PO that does not exist. */
    public static void onInvalidPurchaseOrderReference(String poReference) {
        SwingUtilities.invokeLater(() -> {
            try {
                WarehouseMgmtSubsystem.INSTANCE
                        .onInvalidPurchaseOrderReference(poReference);
            } catch (Throwable t) { /* absorbed */ }
        });
    }

    /** ID 14 — Called when a task references a product SKU not in the catalog. */
    public static void onInvalidProductReference(String productId) {
        SwingUtilities.invokeLater(() -> {
            try {
                WarehouseMgmtSubsystem.INSTANCE
                        .onInvalidProductReference(productId);
            } catch (Throwable t) { /* absorbed */ }
        });
    }

    /** ID 19 — Called when a returned item has an invalid condition code. */
    public static void onReturnConditionInvalid(String returnId, String conditionCode) {
        SwingUtilities.invokeLater(() -> {
            try {
                WarehouseMgmtSubsystem.INSTANCE
                        .onReturnConditionInvalid(returnId, conditionCode);
            } catch (Throwable t) { /* absorbed */ }
        });
    }

    /** ID 107 — Called when concurrent updates conflict on the same product. */
    public static void onConcurrentUpdateConflict(String productId) {
        SwingUtilities.invokeLater(() -> {
            try {
                WarehouseMgmtSubsystem.INSTANCE
                        .onConcurrentUpdateConflict(productId);
            } catch (Throwable t) { /* absorbed */ }
        });
    }

    /** ID 153 — Called when a stock reservation would push quantity below zero. */
    public static void onStockUnderflow(String productId, int current, int delta) {
        SwingUtilities.invokeLater(() -> {
            try {
                WarehouseMgmtSubsystem.INSTANCE
                        .onStockUnderflow(productId, current, delta);
            } catch (Throwable t) { /* absorbed */ }
        });
    }

    /** ID 155 — Called when a putaway or pick task targets a missing or blocked bin. */
    public static void onBinNotFoundOrBlocked(String binId) {
        SwingUtilities.invokeLater(() -> {
            try {
                WarehouseMgmtSubsystem.INSTANCE
                        .onBinNotFoundOrBlocked(binId);
            } catch (Throwable t) { /* absorbed */ }
        });
    }

    /** ID 156 — Called when two shipments are assigned to the same dock door. */
    public static void onDockDoubleBooking(String dockId,
                                            String shipmentId1, String shipmentId2) {
        SwingUtilities.invokeLater(() -> {
            try {
                WarehouseMgmtSubsystem.INSTANCE
                        .onDockDoubleBooking(dockId, shipmentId1, shipmentId2);
            } catch (Throwable t) { /* absorbed */ }
        });
    }

    /** ID 313 — Called when RFID physical scan count differs from system count. */
    public static void onCycleCountDiscrepancy(String locationId,
                                                int systemQty, int physicalQty) {
        SwingUtilities.invokeLater(() -> {
            try {
                WarehouseMgmtSubsystem.INSTANCE
                        .onCycleCountDiscrepancy(locationId, systemQty, physicalQty);
            } catch (Throwable t) { /* absorbed */ }
        });
    }

    /** ID 314 — Called when received quantity differs from PO quantity. */
    public static void onGrnQtyMismatch(String grnId, int poQty, int receivedQty) {
        SwingUtilities.invokeLater(() -> {
            try {
                WarehouseMgmtSubsystem.INSTANCE
                        .onGrnQtyMismatch(grnId, poQty, receivedQty);
            } catch (Throwable t) { /* absorbed */ }
        });
    }

    /** ID 407 — Called when damaged goods are detected during inbound QC or cold chain. */
    public static void onDamagedGoodsDetected(String productId, String binId) {
        SwingUtilities.invokeLater(() -> {
            try {
                WarehouseMgmtSubsystem.INSTANCE
                        .onDamagedGoodsDetected(productId, binId);
            } catch (Throwable t) { /* absorbed */ }
        });
    }

    /** Local fallback when Subsystem 17 JAR is completely offline. */
    private static void localFallbackLog(Exception e) {
        WarehouseTerminalView.printError("SUBSYSTEM-17-FALLBACK",
                "Exception Handler offline. Local log:", e);
    }
}