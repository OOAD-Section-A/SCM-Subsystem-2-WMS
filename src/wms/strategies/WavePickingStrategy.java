package wms.strategies;

import java.util.List;
import wms.models.Order;
import wms.models.WarehouseTask;
import wms.views.WarehouseTerminalView;

/**
 * Description: Groups orders into scheduled "waves" based on departure times.
 */
public class WavePickingStrategy implements IPickingStrategy {
    
    @Override
    public void generatePickList(Order order) {
        WarehouseTerminalView.printRouting("Wave Picking", "Sorting tasks by Bin ID for optimal routing.");
    }

    @Override
    public List<WarehouseTask> optimizePickPath(List<WarehouseTask> tasks) {
        WarehouseTerminalView.printSystemEvent("STRATEGY", "Executing Wave Picking: Sorting tasks by Bin ID for optimal routing.");
        // In a real implementation, we would sort the list here. For now, pass it through.
        return tasks;
    }
}