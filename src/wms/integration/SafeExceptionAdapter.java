package wms.integration;

import wms.exceptions.BinCapacityExceededException;
import wms.exceptions.InsufficientStockException;
import wms.views.WarehouseTerminalView;

// Assuming the external JAR provides a WarehouseManagementSubsystem.INSTANCE or similar
public class SafeExceptionAdapter {

	private static boolean useExternalJar = true;

	public static void handle(Exception e) {
		if (useExternalJar) {
			try {
				if (e instanceof BinCapacityExceededException) {
					BinCapacityExceededException bce = (BinCapacityExceededException) e;
					WarehouseTerminalView.printSystemEvent("SUBSYSTEM 17",
							"Calling Subsystem 17 onBinCapacityExceeded...");
					// WarehouseManagementSubsystem.INSTANCE.onBinCapacityExceeded(bce.getBinId(), bce.getLimit());
				} else if (e instanceof InsufficientStockException) {
					InsufficientStockException ise = (InsufficientStockException) e;
					WarehouseTerminalView.printSystemEvent("SUBSYSTEM 17",
							"Calling Subsystem 17 onInsufficientStockForPick...");
					// WarehouseManagementSubsystem.INSTANCE.onInsufficientStockForPick(ise.getProductId(), ise.getRequested(), ise.getAvailable());
				} else {
					WarehouseTerminalView.printSystemEvent("SUBSYSTEM 17", "Calling generic error logger...");
				}
				throw new NoClassDefFoundError("WarehouseManagementSubsystem");
			} catch (Throwable t) {
				useExternalJar = false;
				WarehouseTerminalView.printWarning("CIRCUIT BREAKER", "Subsystem 17 offline.");
				localFallbackLog(e);
			}
			return;
		}

		localFallbackLog(e);
	}

	private static void localFallbackLog(Exception e) {
		WarehouseTerminalView.printError("FALLBACK", "Local exception log:", e);
	}

	public static void main(String[] args) {
		try {
			throw new wms.exceptions.WmsCoreException("Simulated Bin Overflow");
		} catch (Exception e) {
			SafeExceptionAdapter.handle(e);
		}

		try {
			throw new wms.exceptions.WmsCoreException("Simulated Bin Overflow");
		} catch (Exception e) {
			SafeExceptionAdapter.handle(e);
		}
	}
}
