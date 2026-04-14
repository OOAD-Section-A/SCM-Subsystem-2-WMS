package wms.strategies;

import wms.models.Order;

/**
 * Description: Behavioral Strategy interface for outbound picking operations.
 */
public interface IPickingStrategy {
    void generatePickList(Order order);
}