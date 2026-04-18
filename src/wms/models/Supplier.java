package wms.models;

/**
 * Description: Aligned with Database Supplier table.
 */

public class Supplier {
    private String supplierId;
    private String name;
    private int avgLeadTime;
    private double reliabilityScore;

    public Supplier(String supplierId, String name, int avgLeadTime, double reliabilityScore) {
        this.supplierId = supplierId;
        this.name = name;
        this.avgLeadTime = avgLeadTime;
        this.reliabilityScore = reliabilityScore;
    }

    public String getSupplierId() { return supplierId; }
    public String getName() { return name; }
    public int getAvgLeadTime() { return avgLeadTime; }
    public double getReliabilityScore() { return reliabilityScore; }
}