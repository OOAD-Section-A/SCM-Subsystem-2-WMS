package wms.views;

public class WarehouseTerminalView {
	private static final String ANSI_RESET = "\u001B[0m";
	private static final String ANSI_YELLOW = "\u001B[33m";
	private static final String ANSI_RED = "\u001B[31m";

	private WarehouseTerminalView() {
		// Utility class: prevent instantiation.
	}

	public static void printSystemEvent(String component, String message) {
		System.out.println("[SYSTEM | " + component + "] " + message);
	}

	public static void printTaskExecution(String taskId, String status) {
		System.out.println("[EXECUTION] Task " + taskId + " is now " + status);
	}

	public static void printRouting(String strategyName, String message) {
		System.out.println("[ROUTING | " + strategyName + "] " + message);
	}

	public static void printWarning(String component, String message) {
		System.out.println(ANSI_YELLOW + "[WARNING | " + component + "] " + message + ANSI_RESET);
	}

	public static void printError(String component, String message, Exception e) {
		System.err.println(ANSI_RED + "[CRITICAL | " + component + "] " + message + ANSI_RESET);
		System.err.println(ANSI_RED + e.getMessage() + ANSI_RESET);
	}
}
