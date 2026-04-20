package wms.integration;

import java.util.ArrayList;
import java.util.List;
import wms.models.WarehouseTask;
import wms.views.WarehouseTerminalView;

public class InMemoryWMSRepository implements IWMSRepository {
    
    // In a full implementation, we would populate this cache
    private final List<WarehouseTask> localCache = new ArrayList<>();

    @Override
    public List<WarehouseTask> fetchPendingTasks() {
        WarehouseTerminalView.printSystemEvent("IN-MEMORY DB", "Fetching pending tasks from local cache...");
        return localCache; 
    }

    @Override
    public void updateTaskStatus(String taskId, String newStatus) {
        WarehouseTerminalView.printSystemEvent("IN-MEMORY DB", "Updating task " + taskId + " to " + newStatus + " in local cache...");
    }
}