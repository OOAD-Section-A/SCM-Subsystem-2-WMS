package wms.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import wms.models.WarehouseTask;

public class WavePickingStrategy implements IPickingStrategy {
	@Override
	public List<WarehouseTask> optimizePickPath(List<WarehouseTask> pendingTasks) {
		System.out.println("[STRATEGY] Executing Wave Picking: Sorting tasks by Bin ID for optimal routing.");

		List<WarehouseTask> optimizedTasks = new ArrayList<>(pendingTasks);
		optimizedTasks.sort(Comparator.comparing(WarehouseTask::getTargetBinId));

		return optimizedTasks;
	}

	public static void main(String[] args) {
		List<WarehouseTask> tasks = new ArrayList<>();
		tasks.add(new WarehouseTask("T-1", "PICK", "P-10", "C3", "PENDING"));
		tasks.add(new WarehouseTask("T-2", "PICK", "P-20", "A1", "PENDING"));
		tasks.add(new WarehouseTask("T-3", "PICK", "P-30", "B2", "PENDING"));

		WavePickingStrategy strategy = new WavePickingStrategy();
		List<WarehouseTask> sortedTasks = strategy.optimizePickPath(tasks);

		for (WarehouseTask task : sortedTasks) {
			System.out.println(task.getTargetBinId());
		}
	}
}
