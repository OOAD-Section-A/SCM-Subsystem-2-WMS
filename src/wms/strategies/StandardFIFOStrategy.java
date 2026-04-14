package wms.strategies;

import wms.models.Product;

/**
 * Description: Standard First-In-First-Out algorithm for dry supermarket goods.
 */
public class StandardFIFOStrategy implements IPutawayStrategy {
    @Override
    public String determineStorageBin(Product product) {
        System.out.println("Strategy [StandardFIFO]: Routing '" + product.getName() + "' to standard dry goods racking.");
        return "ZONE-DRY-BIN-99"; 
    }
}
