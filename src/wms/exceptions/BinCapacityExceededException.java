package wms.exceptions;

public class BinCapacityExceededException extends WmsCoreException {

	private final String binId;
	private final double limit;

	public BinCapacityExceededException(String binId, double limit, String message) {
		super(message);
		this.binId = binId;
		this.limit = limit;
	}

	public String getBinId() {
		return binId;
	}

	public double getLimit() {
		return limit;
	}
}
