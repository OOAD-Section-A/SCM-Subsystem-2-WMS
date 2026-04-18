package wms.models;

/**
 * Description: Quality Inspection Report (QIR). Records the QC outcome.
 */
public class QualityInspection {
    private String inspectionId;
    private String grnId;
    private String productId;
    private int passedQty;
    private int failedQty;
    private String remarks;

    public QualityInspection(String inspectionId, String grnId, String productId, int passedQty, int failedQty, String remarks) {
        this.inspectionId = inspectionId;
        this.grnId = grnId;
        this.productId = productId;
        this.passedQty = passedQty;
        this.failedQty = failedQty;
        this.remarks = remarks;
    }

    public void printReport() {
        System.out.println(" -> [QIR REPORT] ID: " + inspectionId + " | Passed: " + passedQty + " | Failed: " + failedQty + " | Remarks: " + remarks);
    }
}
