package wms.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import wms.exceptions.WmsCoreException;
import wms.integration.IWarehousePackingIntegration;
import wms.integration.IWMSRepository;
import wms.integration.SafeExceptionAdapter;
import wms.integration.SCMDatabaseAdapter;
import wms.integration.Subsystem14PackingAdapter;
import wms.models.WarehouseTask;
import wms.strategies.IPickingStrategy;
import wms.strategies.WavePickingStrategy;
import wms.views.WarehouseTerminalView;

public class OutboundTaskController implements Runnable {
	private final IWMSRepository repository;
	private final IPickingStrategy pickingStrategy = new WavePickingStrategy();
	private final IWarehousePackingIntegration packingIntegration = new Subsystem14PackingAdapter();
	private final Map<String, Integer> retryCounts = new HashMap<>();

	public OutboundTaskController(IWMSRepository repository) {
		this.repository = repository;
	}

	@Override
	public void run() {
		for (int i = 0; i < 4; i++) {
			List<WarehouseTask> tasks = repository.fetchPendingTasks();
			List<WarehouseTask> optimizedTasks = pickingStrategy.optimizePickPath(tasks);

			if (!optimizedTasks.isEmpty()) {
				for (WarehouseTask task : optimizedTasks) {
					try {
						WarehouseTerminalView.printSystemEvent("POLLER", "Found task: " + task.getTaskId());
						if (task.getTaskId().equals("T-ERR")) {
							throw new wms.exceptions.InsufficientStockException(task.getProductId(), 50, 10, "Not enough stock for pick task");
						} else {
							WarehouseTerminalView.printTaskExecution(task.getTaskId(), "COMPLETED");
							repository.updateTaskStatus(task.getTaskId(), "COMPLETED");
							packingIntegration.createPackingJob(task.getTaskId(), "PACK-STATION-1");
						}
					} catch (Exception e) {
						int count = retryCounts.getOrDefault(task.getTaskId(), 0) + 1;
						retryCounts.put(task.getTaskId(), count);

						if (count >= 3) {
							WarehouseTerminalView.printError("DLQ", "Task " + task.getTaskId() + " failed 3 times. Routing to Subsystem 17.", e);
							repository.updateTaskStatus(task.getTaskId(), "FAILED");
							SafeExceptionAdapter.handle(e);
						} else {
							WarehouseTerminalView.printWarning("DLQ", "Task " + task.getTaskId() + " failed. Retrying attempt " + count);
						}
					}
				}
			}

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public static void main(String[] args) {
		SCMDatabaseAdapter adapter = new SCMDatabaseAdapter();
		OutboundTaskController controller = new OutboundTaskController(adapter);
		Thread pollerThread = new Thread(controller);
		pollerThread.start();
	}
}
