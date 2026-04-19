package wms.services;

import wms.integration.IDemandForecastingIntegration;
import wms.integration.Subsystem8ForecastAdapter;
import wms.views.WarehouseTerminalView;

public class SlottingOptimizerService {
	private final IDemandForecastingIntegration forecastIntegration;

	public SlottingOptimizerService(IDemandForecastingIntegration forecastIntegration) {
		this.forecastIntegration = forecastIntegration;
	}

	public void evaluateAndOptimizeSlotting(String productId, String currentBin, String fastPickBin) {
		boolean hasSpike = forecastIntegration.hasUpcomingDemandSpike(productId);

		if (hasSpike) {
			WarehouseTerminalView.printSystemEvent(
					"SLOTTING OPTIMIZER",
					"Demand spike predicted for " + productId
							+ ". Generating preemptive INTERNAL_MOVE task from " + currentBin
							+ " to forward-pick bin: " + fastPickBin);
		} else {
			WarehouseTerminalView.printSystemEvent(
					"SLOTTING OPTIMIZER",
					"No demand spikes for " + productId + ". Current slotting in " + currentBin + " is optimal.");
		}
	}

	public static void main(String[] args) {
		Subsystem8ForecastAdapter adapter = new Subsystem8ForecastAdapter();
		SlottingOptimizerService service = new SlottingOptimizerService(adapter);
		service.evaluateAndOptimizeSlotting("P-50", "BULK-ZONE-9", "FAST-PICK-1");
	}
}
