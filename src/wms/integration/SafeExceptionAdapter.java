package wms.integration;

public class SafeExceptionAdapter {

	private static boolean useExternalJar = true;

	public static void handle(Exception e) {
		if (useExternalJar) {
			try {
				System.out.println("[ATTEMPTING EXTERNAL SUBSYSTEM 17] Routing exception to SCMExceptionHandler...");
				// SCMExceptionHandler.INSTANCE.handle(e)
				throw new NoClassDefFoundError("SCMExceptionHandler");
			} catch (Throwable t) {
				useExternalJar = false;
				System.out.println("[CIRCUIT BREAKER TRIPPED] Subsystem 17 offline.");
				localFallbackLog(e);
			}
			return;
		}

		localFallbackLog(e);
	}

	private static void localFallbackLog(Exception e) {
		System.err.println("[LOCAL FALLBACK LOG]");
		System.err.println(e.getMessage());
		e.printStackTrace(System.err);
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
