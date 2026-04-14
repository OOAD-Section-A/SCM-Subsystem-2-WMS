package wms.models;

/**
 * Description: Categorizes products to determine their routing inside the WMS.
 */
public enum ProductCategory {
    PERISHABLE_COLD, // Requires temperature control (e.g., Dairy, Meat)
    DRY_GOODS,       // Standard racking (e.g., Cereal, Canned goods)
    HIGH_VALUE       // Requires secure cage (e.g., Expensive electronics/alcohol)
}
