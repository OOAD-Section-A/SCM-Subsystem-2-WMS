package wms.models;

/**
 * Description: Represents a physical item entering the warehouse.
 */
public class Product {
    private String sku;
    private String name;
    private ProductCategory category;

    public Product(String sku, String name, ProductCategory category) {
        this.sku = sku;
        this.name = name;
        this.category = category;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public ProductCategory getCategory() { return category; }
}
