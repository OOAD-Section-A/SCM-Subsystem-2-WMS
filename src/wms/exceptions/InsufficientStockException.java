package wms.exceptions;

public class InsufficientStockException extends WmsCoreException {

	private final String productId;
	private final int requested;
	private final int available;

	public InsufficientStockException(String productId, int requested, int available, String message) {
		super(message);
		this.productId = productId;
		this.requested = requested;
		this.available = available;
	}

	public String getProductId() {
		return productId;
	}

	public int getRequested() {
		return requested;
	}

	public int getAvailable() {
		return available;
	}
}
