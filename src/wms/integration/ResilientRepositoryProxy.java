package wms.integration;

import java.util.List;
import wms.models.WarehouseTask;
import wms.views.WarehouseTerminalView;

public class ResilientRepositoryProxy implements IWMSRepository {
    
    private final IWMSRepository primary;
    private final IWMSRepository fallback;

    public ResilientRepositoryProxy(IWMSRepository primary, IWMSRepository fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public List<WarehouseTask> fetchPendingTasks() {
        try {
            return primary.fetchPendingTasks();
        } catch (Exception e) {
            WarehouseTerminalView.printWarning("CIRCUIT BREAKER", "Primary DB unreachable. Rerouting fetch to In-Memory Fallback...");
            return fallback.fetchPendingTasks();
        }
    }

    @Override
    public void updateTaskStatus(String taskId, String newStatus) {
        try {
            primary.updateTaskStatus(taskId, newStatus);
        } catch (Exception e) {
            WarehouseTerminalView.printWarning("CIRCUIT BREAKER", "Primary DB unreachable. Rerouting update to In-Memory Fallback...");
            fallback.updateTaskStatus(taskId, newStatus);
        }
    }
}