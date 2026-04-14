package wms.strategies;

import wms.models.Order;

/**
 * Description: Groups orders into scheduled "waves" based on departure times.
 */
public class WavePickingStrategy implements IPickingStrategy {
    @Override
    public void generatePickList(Order order) {
        System.out.println("Strategy [WavePicking]: Scheduling Order " + order.getOrderId() + " into the 14:00 PM Dispatch Wave.");
        System.out.println(" -> Generating sequential routing path for worker...");
    }
}