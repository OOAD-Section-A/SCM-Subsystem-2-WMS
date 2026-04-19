package wms.integration;

import java.util.List;
import wms.models.WarehouseTask;

public interface IWMSRepository {
	List<WarehouseTask> fetchPendingTasks();

	void updateTaskStatus(String taskId, String newStatus);
}
