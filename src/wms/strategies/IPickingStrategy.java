package wms.strategies;

import java.util.List;
import wms.models.Order;
import wms.models.WarehouseTask;

public interface IPickingStrategy {
    // Used by OrderPickingEngine (Legacy/Inbound)
    void generatePickList(Order order);
    
    // Used by OutboundTaskController (New Asynchronous Poller)
    List<WarehouseTask> optimizePickPath(List<WarehouseTask> tasks);
}