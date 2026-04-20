package wms.services.integration;

import wms.models.Product;

/**
 * Interface defining the cross-docking rules in WMS.
 */
public interface ICrossDockingService {
    /**
     * Checks if a newly arrived product has an urgent backorder and should bypass storage.
     * @param product The product scanned at inbound dock.
     * @return true if it should be cross-docked to shipping.
     */
    boolean evaluateCrossDocking(Product product);
}
