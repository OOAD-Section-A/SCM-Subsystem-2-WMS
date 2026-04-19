package wms.integration;

public interface IWarehousePackingIntegration {
	boolean createPackingJob(String taskId, String targetBinId);
}
