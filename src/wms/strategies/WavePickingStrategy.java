package wms.strategies;

import wms.models.Order;
import wms.views.WarehouseTerminalView;

/**
 * Description: Groups orders into scheduled "waves" based on departure times.
 */
public class WavePickingStrategy implements IPickingStrategy {
    @Override
    public void generatePickList(Order order) {
        WarehouseTerminalView.printRouting("Wave Picking", "Sorting tasks by Bin ID for optimal routing.");
    }
}