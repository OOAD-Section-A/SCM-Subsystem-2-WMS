package wms.integration;

import wms.views.WarehouseTerminalView;

public class Subsystem8ForecastAdapter implements IDemandForecastingIntegration {
	private boolean isSubsystem8Online = true;

	@Override
	public boolean hasUpcomingDemandSpike(String productId) {
		if (isSubsystem8Online) {
			try {
				WarehouseTerminalView.printSystemEvent("FORECAST ADAPTER",
						"Querying Subsystem 8 for demand spikes on Product " + productId + "...");
				throw new RuntimeException("Simulated Subsystem 8 Timeout");
			} catch (Exception e) {
				isSubsystem8Online = false;
				WarehouseTerminalView.printWarning("CIRCUIT BREAKER",
						"Subsystem 8 Demand Forecasting is offline. Defaulting to standard slotting.");
				return false;
			}
		}

		return false;
	}

	public static void main(String[] args) {
		Subsystem8ForecastAdapter adapter = new Subsystem8ForecastAdapter();
		adapter.hasUpcomingDemandSpike("P-50");
		adapter.hasUpcomingDemandSpike("P-50");
	}
}

