package wms.models;

/**
 * Description: Captures mismatches found during the 3-Way Match process.
 */
public class Discrepancy {
    private String type; // QUANTITY, PRICE, DAMAGE
    private String productId;
    private String supplierId;
    private String description;

    public Discrepancy(String type, String productId, String supplierId, String description) {
        this.type = type;
        this.productId = productId;
        this.supplierId = supplierId;
        this.description = description;
    }

    public void printDetails() {
        System.out.println(" -> [DISCREPANCY CAUGHT] Type: " + type + " | SKU: " + productId + " | Details: " + description);
    }
}
