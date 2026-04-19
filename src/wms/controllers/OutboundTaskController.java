package wms.controllers;

import java.util.List;
import wms.integration.IWMSRepository;
import wms.integration.SCMDatabaseAdapter;
import wms.models.WarehouseTask;

public class OutboundTaskController implements Runnable {
	private final IWMSRepository repository;

	public OutboundTaskController(IWMSRepository repository) {
		this.repository = repository;
	}

	@Override
	public void run() {
		for (int i = 0; i < 3; i++) {
			List<WarehouseTask> tasks = repository.fetchPendingTasks();

			if (!tasks.isEmpty()) {
				for (WarehouseTask task : tasks) {
					System.out.println("[POLLER] Found task: " + task.getTaskId());
					repository.updateTaskStatus(task.getTaskId(), "COMPLETED");
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
