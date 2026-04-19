package wms.integration;

public class Subsystem14PackingAdapter implements IWarehousePackingIntegration {
	private boolean isSubsystem14Online = true;

	@Override
	public boolean createPackingJob(String taskId, String targetBinId) {
		if (isSubsystem14Online) {
			try {
				System.out.println("[PACKING ADAPTER] Attempting to hand off Task " + taskId + " in Bin " + targetBinId + " to Subsystem 14...");
				throw new RuntimeException("Simulated Subsystem 14 Timeout");
			} catch (Exception e) {
				isSubsystem14Online = false;
				System.out.println("[CIRCUIT BREAKER] Subsystem 14 is offline or timed out.");
				handleFallback(taskId, targetBinId);
				return false;
			}
		}

		handleFallback(taskId, targetBinId);
		return false;
	}

	private void handleFallback(String taskId, String targetBinId) {
		System.out.println("[FALLBACK] Subsystem 14 unreachable. Pallet for Task " + taskId + " staged at Dock manually.");
	}

	public static void main(String[] args) {
		Subsystem14PackingAdapter adapter = new Subsystem14PackingAdapter();
		adapter.createPackingJob("T-200", "PACK-STATION-1");
		adapter.createPackingJob("T-200", "PACK-STATION-1");
	}
}
