package wms.demo;

import wms.exceptions.BinCapacityExceededException;
import wms.exceptions.InsufficientStockException;
import wms.integration.SafeExceptionAdapter;
import wms.views.WarehouseTerminalView;

/**
 * Standalone demonstration class for WMS Exception Handling integration.
 *
 * PURPOSE:
 * Triggers all 12 WMS-registered exceptions to demonstrate:
 * 1. Correct routing through SafeExceptionAdapter to Subsystem 17
 * 2. Blocking popup appearing for each exception
 * 3. Each exception logged in Windows Event Viewer under SCM-WarehouseMgmt
 * 4. All exceptions visible in the Exception Viewer GUI
 *
 * HOW TO RUN:
 * Step 1 — Launch the Exception Viewer GUI in a separate terminal:
 *   cd lib
 *   java -cp ".;scm-exception-viewer-gui.jar;scm-exception-handler-v3.jar;jna-5.18.1.jar;jna-platform-5.18.1.jar" com.scm.gui.ExceptionViewerGUI
 *
 * Step 2 — Run this demo:
 *   mvn exec:java -Dexec.mainClass="wms.demo.ExceptionDemo"
 *
 * Step 3 — Click OK on each popup to proceed to the next exception.
 *
 * Step 4 — After all 12 exceptions fire, click Refresh Now in the GUI
 *   to see all exceptions listed in the table.
 *
 * PREREQUISITE:
 * Registry entry must be set up once as Administrator:
 *   reg add "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\EventLog\Application\SCM-WarehouseMgmt"
 *     /v EventMessageFile /t REG_SZ
 *     /d "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\EventLogMessages.dll" /f
 *
 * NOTE: Each exception triggers a blocking popup — click OK before the next fires.
 * This is by design in Subsystem 17 to ensure exceptions are acknowledged.
 */
public class ExceptionDemo {

    public static void main(String[] args) throws InterruptedException {

        printBanner();

        // Give Swing time to initialize before first popup
        Thread.sleep(500);

        // ── EXCEPTION 1: ID 154 — BIN_CAPACITY_EXCEEDED ──────────────────────
        printStep(1, "BIN_CAPACITY_EXCEEDED (ID 154)",
                "Bin ZONE-COLD-BIN-01 is at full capacity (500 kg limit)");
        try {
            throw new BinCapacityExceededException("ZONE-COLD-BIN-01", 500.0,
                    "Bin ZONE-COLD-BIN-01 has exceeded its maximum capacity of 500 kg");
        } catch (BinCapacityExceededException e) {
            SafeExceptionAdapter.handle(e);
        }
        pause();

        // ── EXCEPTION 2: ID 152 — INSUFFICIENT_STOCK_FOR_PICK ────────────────
        printStep(2, "INSUFFICIENT_STOCK_FOR_PICK (ID 152)",
                "Pick task for SKU-DAIRY-1 requires 100 units but only 10 available");
        try {
            throw new InsufficientStockException("SKU-DAIRY-1", 100, 10,
                    "Insufficient stock for pick task on SKU-DAIRY-1");
        } catch (InsufficientStockException e) {
            SafeExceptionAdapter.handle(e);
        }
        pause();

        // ── EXCEPTION 3: ID 13 — INVALID_PURCHASE_ORDER_REFERENCE ────────────
        printStep(3, "INVALID_PURCHASE_ORDER_REFERENCE (ID 13)",
                "GRN references PO-9999 which does not exist in the system");
        SafeExceptionAdapter.onInvalidPurchaseOrderReference("PO-9999");
        pause();

        // ── EXCEPTION 4: ID 14 — INVALID_PRODUCT_REFERENCE ───────────────────
        printStep(4, "INVALID_PRODUCT_REFERENCE (ID 14)",
                "Pick task references product SKU-UNKNOWN-99 not found in catalog");
        SafeExceptionAdapter.onInvalidProductReference("SKU-UNKNOWN-99");
        pause();

        // ── EXCEPTION 5: ID 19 — RETURN_CONDITION_INVALID ────────────────────
        printStep(5, "RETURN_CONDITION_INVALID (ID 19)",
                "Returned item RET-001 has unrecognised condition code XYZ");
        SafeExceptionAdapter.onReturnConditionInvalid("RET-001", "XYZ");
        pause();

        // ── EXCEPTION 6: ID 107 — CONCURRENT_UPDATE_CONFLICT ─────────────────
        printStep(6, "CONCURRENT_UPDATE_CONFLICT (ID 107)",
                "Two workers simultaneously updating stock for SKU-BREAD-3");
        SafeExceptionAdapter.onConcurrentUpdateConflict("SKU-BREAD-3");
        pause();

        // ── EXCEPTION 7: ID 153 — STOCK_UNDERFLOW ────────────────────────────
        printStep(7, "STOCK_UNDERFLOW (ID 153)",
                "Reservation of 50 units would push SKU-MILK-2 below zero (current: 30)");
        SafeExceptionAdapter.onStockUnderflow("SKU-MILK-2", 30, 50);
        pause();

        // ── EXCEPTION 8: ID 155 — BIN_NOT_FOUND_OR_BLOCKED ───────────────────
        printStep(8, "BIN_NOT_FOUND_OR_BLOCKED (ID 155)",
                "Putaway task targeting bin ZONE-DRY-BIN-99 — bin is blocked for maintenance");
        SafeExceptionAdapter.onBinNotFoundOrBlocked("ZONE-DRY-BIN-99");
        pause();

        // ── EXCEPTION 9: ID 156 — DOCK_DOUBLE_BOOKING ────────────────────────
        printStep(9, "DOCK_DOUBLE_BOOKING (ID 156)",
                "Dock-B already assigned to SHIP-001, cannot assign SHIP-002");
        SafeExceptionAdapter.onDockDoubleBooking("Dock-B", "SHIP-001", "SHIP-002");
        pause();

        // ── EXCEPTION 10: ID 313 — CYCLE_COUNT_DISCREPANCY ───────────────────
        printStep(10, "CYCLE_COUNT_DISCREPANCY (ID 313)",
                "RFID scan found 45 units in ZONE-DRY-BIN-12, system shows 50");
        SafeExceptionAdapter.onCycleCountDiscrepancy("ZONE-DRY-BIN-12", 50, 45);
        pause();

        // ── EXCEPTION 11: ID 314 — GRN_QTY_MISMATCH ─────────────────────────
        printStep(11, "GRN_QTY_MISMATCH (ID 314)",
                "GRN-2024-001 received 45 units but PO-1001 ordered 50 units");
        SafeExceptionAdapter.onGrnQtyMismatch("GRN-2024-001", 50, 45);
        pause();

        // ── EXCEPTION 12: ID 407 — DAMAGED_GOODS_DETECTED ────────────────────
        printStep(12, "DAMAGED_GOODS_DETECTED (ID 407)",
                "Cold chain breach detected for SKU-DAIRY-1 at Dock-A — temperature exceeded");
        SafeExceptionAdapter.onDamagedGoodsDetected("SKU-DAIRY-1", "Dock-A");
        pause();

        // ── DEMO COMPLETE ─────────────────────────────────────────────────────
        Thread.sleep(1000);
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "============================================================");
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "All 12 WMS exceptions demonstrated successfully.");
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "Click 'Refresh Now' in the Exception Viewer GUI to see all entries.");
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "Check Windows Event Viewer under: Windows Logs > Application");
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "Filter by Source: SCM-WarehouseMgmt");
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "============================================================");
    }

    /**
     * Prints the demo banner at startup.
     */
    private static void printBanner() {
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "============================================================");
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "WMS Exception Handling Demo — Subsystem 17 Integration");
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "Demonstrating all 12 registered WMS exception types");
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "Each exception will trigger a popup — click OK to continue");
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "============================================================");
    }

    /**
     * Prints a labelled step header before each exception is fired.
     */
    private static void printStep(int stepNum, String exceptionName, String scenario) {
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "------------------------------------------------------------");
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "Exception " + stepNum + "/12: " + exceptionName);
        WarehouseTerminalView.printSystemEvent("EXCEPTION-DEMO",
                "Scenario: " + scenario);
    }

    /**
     * Pauses between exceptions to allow the Swing popup to appear and
     * be acknowledged before the next exception fires.
     * SwingUtilities.invokeLater is asynchronous — this pause ensures
     * the popup has time to render before the next one is queued.
     */
    private static void pause() throws InterruptedException {
        Thread.sleep(800);
    }
}
