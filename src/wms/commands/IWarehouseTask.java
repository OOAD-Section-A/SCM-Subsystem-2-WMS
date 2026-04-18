package wms.commands;

/**
 * Description: Behavioral Pattern (Command). 
 * Interface for all queued worker tasks in the warehouse.
 */
public interface IWarehouseTask {
    void execute();
}
