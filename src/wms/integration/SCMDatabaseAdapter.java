package wms.integration;

import java.util.ArrayList;
import java.util.List;
import wms.models.WarehouseTask;

public class SCMDatabaseAdapter implements IWMSRepository {
	// In reality, this will wrap Subsystem 15's SupplyChainDatabaseFacade

	@Override
	public List<WarehouseTask> fetchPendingTasks() {
		List<WarehouseTask> tasks = new ArrayList<>();
		tasks.add(new WarehouseTask("T-100", "PICK", "P-50", "A1", "PENDING"));
		tasks.add(new WarehouseTask("T-ERR", "PICK", "P-99", "B2", "PENDING"));
		return tasks;
	}

	@Override
	public void updateTaskStatus(String taskId, String newStatus) {
		System.out.println("[DB ADAPTER] Updating task " + taskId + " to status: " + newStatus);
	}

	public static void main(String[] args) {
		SCMDatabaseAdapter adapter = new SCMDatabaseAdapter();
		List<WarehouseTask> tasks = adapter.fetchPendingTasks();

		if (!tasks.isEmpty()) {
			System.out.println("Fetched task ID: " + tasks.get(0).getTaskId());
		}

		adapter.updateTaskStatus("T-100", "ACTIVE");
	}
}
