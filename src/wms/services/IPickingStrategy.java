package wms.services;

import java.util.List;
import wms.models.WarehouseTask;

public interface IPickingStrategy {
	List<WarehouseTask> optimizePickPath(List<WarehouseTask> pendingTasks);
}
