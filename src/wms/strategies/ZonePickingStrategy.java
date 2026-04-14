package wms.strategies;

import wms.models.Order;

/**
 * Description: Splits the order based on warehouse zones (e.g., Cold vs. Dry).
 */
public class ZonePickingStrategy implements IPickingStrategy {
    @Override
    public void generatePickList(Order order) {
        System.out.println("Strategy [ZonePicking]: Dividing Order " + order.getOrderId() + " across physical zones.");
        System.out.println(" -> Task 1 assigned to Cold Zone worker.");
        System.out.println(" -> Task 2 assigned to Dry Zone worker.");
    }
}