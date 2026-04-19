package wms.integration;

import wms.views.WarehouseTerminalView;

public class SafeExceptionAdapter {

	private static boolean useExternalJar = true;

	public static void handle(Exception e) {
		if (useExternalJar) {
			try {
				WarehouseTerminalView.printSystemEvent("SUBSYSTEM 17", "Routing exception to SCMExceptionHandler...");
				// SCMExceptionHandler.INSTANCE.handle(e)
				throw new NoClassDefFoundError("SCMExceptionHandler");
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
