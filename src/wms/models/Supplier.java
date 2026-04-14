package wms.models;

/**
 * Description: Represents an external vendor supplying goods to the warehouse.
 * Built as an essential dependency since there is no standalone Procurement subsystem.
 */
public class Supplier {
    private String supplierId;
    private String name;

    public Supplier(String supplierId, String name) {
        this.supplierId = supplierId;
        this.name = name;
    }

    public String getName() { return name; }
}