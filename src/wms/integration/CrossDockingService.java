package wms.services.integration;

import wms.models.Product;
import java.util.HashSet;
import java.util.Set;

public class CrossDockingService implements ICrossDockingService {
    
    private Set<String> urgentBackorders;

    public CrossDockingService() {
        this.urgentBackorders = new HashSet<>();
    }

    public void addUrgentBackorder(String sku) {
        urgentBackorders.add(sku);
    }

    @Override
    public boolean evaluateCrossDocking(Product product) {
        if (urgentBackorders.contains(product.getSku())) {
            System.out.println("CrossDockingService: [ALERT] Urgent backorder detected for " + product.getSku() + "!");
            System.out.println("CrossDockingService: Rerouting product from Inbound directly to Shipping Dock. Bypassing Putaway.");
            urgentBackorders.remove(product.getSku()); // fulfilled
            return true;
        }
        return false;
    }
}
