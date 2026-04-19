package wms.models;

public class WarehouseTask {
	private String taskId;
	private String taskType;
	private String productId;
	private String targetBinId;
	private String status;

	public WarehouseTask(String taskId, String taskType, String productId, String targetBinId, String status) {
		this.taskId = taskId;
		this.taskType = taskType;
		this.productId = productId;
		this.targetBinId = targetBinId;
		this.status = status;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getTargetBinId() {
		return targetBinId;
	}

	public void setTargetBinId(String targetBinId) {
		this.targetBinId = targetBinId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
