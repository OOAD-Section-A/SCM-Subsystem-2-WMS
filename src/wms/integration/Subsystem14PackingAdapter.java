package wms.integration;

import com.scm.packing.integration.database.DatabaseLayerFactory;
import com.scm.packing.integration.database.IDatabaseLayer;
import com.scm.packing.integration.exceptions.ExceptionDispatcherFactory;
import com.scm.packing.integration.exceptions.IExceptionDispatcher;
import com.scm.packing.integration.warehouse.PackingRequest;
import com.scm.packing.integration.warehouse.PackingRequestItem;
import com.scm.packing.integration.warehouse.PackingStatusResponse;
import com.scm.packing.integration.warehouse.WarehousePackingIntegrationService;
import com.scm.packing.mvc.model.PackingModel;
import com.scm.packing.strategy.PackingStrategyFactory;

import wms.views.WarehouseTerminalView;
import wms.services.WMSLogger;

import java.util.List;

/**
 * Adapter connecting Subsystem 2 (WMS) to Subsystem 14 (Packaging, Repairs, Receipt Management).
 *
 * DESIGN PATTERN — Adapter (Object Adapter):
 * Translates WMS's simple (taskId, targetBinId) task model into Subsystem 14's
 * richer PackingRequest/PackingRequestItem API. WMS core classes never import
 * Subsystem 14 classes directly — all coupling is isolated here.
 *
 * DESIGN PATTERN — Proxy / Circuit Breaker:
 * Tracks whether Subsystem 14 is online. On failure, falls back to manual
 * staging immediately without retrying — the retry logic lives in
 * OutboundTaskController's DLQ mechanism, not here.
 *
 * SOLID — Single Responsibility: handles only WMS-to-Packing translation.
 * SOLID — Open/Closed: implements IWarehousePackingStatusAdapter so status
 * polling can be added without changing IWarehousePackingIntegration.
 *
 * EXCEPTION POLICY: logs via WMSLogger, takes no corrective action.
 *
 * SCENARIO HANDLING:
 * - Subsystem 14 online, license server running → full integration path
 * - Subsystem 14 JAR present but license server down → DatabaseLayerFactory
 *   falls back to FlatFileDatabaseAdapter automatically; integration still works
 * - Subsystem 14 JAR missing or constructor throws → offline fallback path,
 *   task is staged manually via DLQ in OutboundTaskController
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 14:
 * WMS calls createPackingJob() with a taskId and targetBinId.
 * We translate these into a PackingRequest with one PackingRequestItem.
 * Poll getPackingStatus() to check progress. Call getPackedItemsSummary()
 * once status returns "PACKED".
 */
public class Subsystem14PackingAdapter implements IWarehousePackingStatusAdapter {

    // The real Subsystem 14 integration service from the packing JAR.
    private WarehousePackingIntegrationService packingService;

    // Tracks live availability of Subsystem 14.
    private boolean isSubsystem14Online = false;

    /**
     * Initialises the adapter by constructing Subsystem 14's service stack.
     *
     * Uses DatabaseLayerFactory and ExceptionDispatcherFactory from the packing JAR.
     * Both factories auto-detect available dependencies and fall back gracefully,
     * so this constructor does not require the license server to be running.
     *
     * If construction fails entirely (JAR missing, classpath issue), the adapter
     * enters offline mode and all calls fall back to manual staging.
     */
    public Subsystem14PackingAdapter() {
        try {
            // Use Subsystem 14's own factories — they handle their internal
            // fallbacks (SCM DB vs flat file, SCM exceptions vs console logger).
            IDatabaseLayer dbLayer = DatabaseLayerFactory.create();
            IExceptionDispatcher exDispatcher = ExceptionDispatcherFactory.create();

            PackingModel packingModel = new PackingModel(dbLayer, exDispatcher);
            PackingStrategyFactory strategyFactory = new PackingStrategyFactory();

            packingService = new WarehousePackingIntegrationService(packingModel, strategyFactory);
            isSubsystem14Online = true;

            WarehouseTerminalView.printSystemEvent("Subsystem14PackingAdapter",
                    "Subsystem 14 (Packing) connected successfully.");

        } catch (Throwable t) {
            // Catch Throwable — packing JAR can throw NoClassDefFoundError
            // if its own Swing dependencies are absent in a headless environment.
            isSubsystem14Online = false;
            WMSLogger.logError("Subsystem14PackingAdapter.<init>",
                    "Subsystem 14 unavailable: " + t.getMessage());
            WarehouseTerminalView.printWarning("Subsystem14PackingAdapter",
                    "Subsystem 14 OFFLINE. Tasks will be staged manually via DLQ.");
        }
    }

    /**
     * Creates a packing job in Subsystem 14 for the given WMS task.
     *
     * Translates the WMS (taskId, targetBinId) model into a PackingRequest:
     * - taskId becomes the orderId so Subsystem 14 can trace it back to WMS
     * - targetBinId is encoded into the item description for physical traceability
     *
     * Returns false on any failure — OutboundTaskController's retry/DLQ
     * mechanism handles the fallback from there.
     *
     * @param taskId      WMS task identifier (e.g., "T-100")
     * @param targetBinId The staging bin holding the picked goods (e.g., "STAGING-OUT")
     * @return            true if Subsystem 14 accepted the job, false otherwise
     */
    @Override
    public boolean createPackingJob(String taskId, String targetBinId) {
        if (!isSubsystem14Online || packingService == null) {
            handleOfflineFallback(taskId, "Subsystem 14 was offline at startup.");
            return false;
        }

        try {
            WarehouseTerminalView.printSystemEvent("Subsystem14PackingAdapter",
                    "Creating packing job for Task: " + taskId + " | Bin: " + targetBinId);

            // Build the packing request — one item per WMS task.
            // weightKg defaults to 1.0; fragile defaults to false.
            // Real implementation would query the Product model for these values.
            PackingRequestItem item = new PackingRequestItem(
                    taskId,
                    "WMS Task " + taskId + " | Staged at: " + targetBinId,
                    1.0,
                    false
            );

            PackingRequest request = new PackingRequest(taskId, List.of(item));
            String jobId = packingService.createPackingJob(request);

            WarehouseTerminalView.printSystemEvent("Subsystem14PackingAdapter",
                    "Packing job created successfully. Job ID: " + jobId
                            + " | WMS Task: " + taskId);
            return true;

        } catch (Throwable t) {
            // Catch Throwable for the same reason as the constructor.
            isSubsystem14Online = false;
            WMSLogger.logError("Subsystem14PackingAdapter.createPackingJob",
                    "Failed for Task " + taskId + ": " + t.getMessage());
            handleOfflineFallback(taskId, t.getMessage());
            return false;
        }
    }

    /**
     * Polls Subsystem 14 for the current status of a packing job.
     * Returns "NOT_FOUND" or "OFFLINE" if the subsystem is unavailable.
     *
     * @param jobId  Job ID returned by createPackingJob()
     * @return       One of: "PENDING", "PACKING", "PACKED", "FAILED",
     *               "NOT_FOUND", "OFFLINE"
     */
    @Override
    public String getPackingStatus(String jobId) {
        if (!isSubsystem14Online || packingService == null) {
            return "OFFLINE";
        }
        try {
            PackingStatusResponse response = packingService.getPackingStatus(jobId);
            WarehouseTerminalView.printSystemEvent("Subsystem14PackingAdapter",
                    "Status for Job " + jobId + ": " + response.getStatus()
                            + " (" + response.getProgress() + "%)");
            return response.getStatus();
        } catch (Throwable t) {
            WMSLogger.logError("Subsystem14PackingAdapter.getPackingStatus", t.getMessage());
            return "FAILED";
        }
    }

    /**
     * Returns a summary of packed items once the job status is "PACKED".
     * Returns an empty string if the subsystem is offline or the job is not found.
     *
     * @param jobId  The packing job ID
     * @return       Summary string, e.g. "3 items packed in job WMS-PKJ-0001"
     */
    @Override
    public String getPackedItemsSummary(String jobId) {
        if (!isSubsystem14Online || packingService == null) {
            return "";
        }
        try {
            var items = packingService.getPackedItems(jobId);
            if (items == null || items.isEmpty()) {
                return "";
            }
            return items.size() + " item(s) packed in job " + jobId;
        } catch (Throwable t) {
            WMSLogger.logError("Subsystem14PackingAdapter.getPackedItemsSummary", t.getMessage());
            return "";
        }
    }

    /**
     * Fallback handler when Subsystem 14 is unreachable.
     * Logs the manual staging requirement for warehouse floor supervisors.
     * No corrective action taken here per project exception handling policy.
     * The DLQ retry mechanism in OutboundTaskController handles the retry cycle.
     */
    private void handleOfflineFallback(String taskId, String reason) {
        WarehouseTerminalView.printWarning("Subsystem14PackingAdapter",
                "Task " + taskId + " requires MANUAL STAGING. Reason: " + reason);
        WMSLogger.logError("Subsystem14PackingAdapter.fallback",
                "Manual staging required for Task: " + taskId);
    }
}