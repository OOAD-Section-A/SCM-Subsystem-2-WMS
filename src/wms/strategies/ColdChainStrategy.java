package wms.strategies;

import wms.models.Product;

/**
 * Description: Algorithm for routing perishable goods to temperature-controlled zones.
 */
public class ColdChainStrategy implements IPutawayStrategy {
    @Override
    public String determineStorageBin(Product product) {
        System.out.println("Strategy [ColdChain]: Routing '" + product.getName() + "' to Temperature Controlled Zone.");
        // In a real system, this would query Subsystem 15 (Database) for an empty cold bin.
        return "ZONE-COLD-BIN-01"; 
    }
}
