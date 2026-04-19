package wms.models;

/**
 * Description: Aligned with Database Supplier table.
 */

public class Supplier {
    private String supplierId;
    private String name;
    private int avgLeadTime;
    private double reliabilityScore;
    private String status;

    public Supplier(String supplierId, String name, int avgLeadTime, double reliabilityScore) {
        this.supplierId = supplierId;
        this.name = name;
        this.avgLeadTime = avgLeadTime;
        this.reliabilityScore = reliabilityScore;
        this.status = "ACTIVE";
    }

    public String getSupplierId() { return supplierId; }
    public String getName() { return name; }
    public int getAvgLeadTime() { return avgLeadTime; }
    public double getReliabilityScore() { return reliabilityScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}