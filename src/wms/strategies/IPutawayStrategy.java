package wms.strategies;

import wms.models.Product;

/**
 * Description: Behavioral Design Pattern (Strategy).
 * Defines the contract for all storage location algorithms.
 */
public interface IPutawayStrategy {
    String determineStorageBin(Product product);
}
