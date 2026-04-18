package wms.commands;

/**
 * Description: Concrete Command / Composite Pattern.
 * Combines an outbound task with a return-trip task to minimize empty travel (deadheading).
 */
public class InterleavedTask implements IWarehouseTask {
    private String workerId;
    private String dropOffLocation;
    private String returnPickupLocation;

    public InterleavedTask(String workerId, String dropOffLocation, String returnPickupLocation) {
        this.workerId = workerId;
        this.dropOffLocation = dropOffLocation;
        this.returnPickupLocation = returnPickupLocation;
    }

    @Override
    public void execute() {
        System.out.println("\n[TASK EXECUTING] Interleaved Task assigned to Worker: " + workerId);
        System.out.println(" -> Step 1: Putaway pallet at " + dropOffLocation);
        System.out.println(" -> Step 2: (No Deadheading!) On return trip, pick cases from " + returnPickupLocation);
        System.out.println(" -> Task Complete. Worker returning to dispatch.");
    }
}
