package wms.exceptions;

public class WmsCoreException extends RuntimeException {

	public WmsCoreException(String message) {
		super(message);
	}

	public WmsCoreException(String message, Throwable cause) {
		super(message, cause);
	}
}
