package wms.integration;

/**
 * Extends IWarehousePackingIntegration with status polling capabilities.
 *
 * SOLID — Open/Closed: extends without modifying the base interface.
 * SOLID — Interface Segregation: only Subsystem14PackingAdapter and callers
 * that need status polling implement/use this. Simple callers use the base.
 *
 * INTEGRATION NOTE FOR SUBSYSTEM 14 (Packaging, Repairs, Receipt Management):
 * Implement this interface in your WarehousePackingIntegrationService.
 * WMS calls these three methods in sequence:
 *   1. createPackingJob()  — fire and forget, returns true if job accepted
 *   2. getPackingStatus()  — poll until status is "PACKED" or "FAILED"
 *   3. getPackedItems()    — retrieve final item list after status is "PACKED"
 */
public interface IWarehousePackingStatusAdapter extends IWarehousePackingIntegration {

    /**
     * Polls the current status of a packing job.
     *
     * @param jobId  Job ID returned by createPackingJob()
     * @return       Status string — one of: "PENDING", "PACKING", "PACKED", "FAILED", "NOT_FOUND"
     *               Never returns null.
     */
    String getPackingStatus(String jobId);

    /**
     * Returns a summary of packed items once the job status is "PACKED".
     * Only call this after getPackingStatus() returns "PACKED".
     *
     * @param jobId  The packing job ID
     * @return       Human-readable summary string of packed items.
     *               Returns empty string if job is not yet packed or not found.
     */
    String getPackedItemsSummary(String jobId);
}
