package wms.exceptions;

/**
 * Description: Base custom exception for the Warehouse Management Subsystem.
 */
public class WMSException extends Exception {
    public WMSException(String message) {
        super(message);
    }
}
