package wms.strategies;

import java.util.List;
import wms.models.Order;
import wms.models.WarehouseTask;
import wms.views.WarehouseTerminalView;

/**
 * Description: Splits the order based on warehouse zones (e.g., Cold vs. Dry).
 */
public class ZonePickingStrategy implements IPickingStrategy {
    
    @Override
    public void generatePickList(Order order) {
        WarehouseTerminalView.printRouting("ZonePicking", "Dividing Order " + order.getOrderId() + " across physical zones.");
    }

    @Override
    public List<WarehouseTask> optimizePickPath(List<WarehouseTask> tasks) {
        WarehouseTerminalView.printSystemEvent("STRATEGY", "Executing Zone Picking: Grouping tasks by Zone classification.");
        return tasks;
    }
}