package wms.services;

import wms.models.Order;
import wms.strategies.IPickingStrategy;

/**
 * Description: The Context class for the Picking Strategy. 
 * Executes the physical workflow after inventory is digitally reserved.
 */
public class OrderPickingEngine {
    private IPickingStrategy pickingStrategy;

    public void setPickingStrategy(IPickingStrategy strategy) {
        this.pickingStrategy = strategy;
    }

    public void executePicking(Order order) {
        if (pickingStrategy == null) {
            throw new IllegalStateException("Picking strategy not set.");
        }
        System.out.println("OrderPickingEngine: Initiating floor pick for Order " + order.getOrderId());
        pickingStrategy.generatePickList(order);
    }
}