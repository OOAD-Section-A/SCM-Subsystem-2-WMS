package wms.services;

/**
 * Description: Bridge to Subsystem 17 (Exception Handling).
 * Exceptions are ONLY logged, with no corrective action taken here.
 */
public class WMSLogger {
    public static void logError(String context, String errorMessage) {
        // In the final integration, this will call Subsystem 17's API.
        System.err.println("[SUBSYSTEM-17 LOG] Error in " + context + ": " + errorMessage);
    }
}
