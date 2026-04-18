package wms.services;

import wms.models.Supplier;
import java.util.List;

/**
 * Description: Implements the Vendor Selection Logic formula
 */
public class VendorSelectionEngine {
    
    // Weights defined
    private static final double WQ = 0.35; // Quality
    private static final double WD = 0.30; // Delivery
    private static final double WP = 0.20; // Price
    private static final double WS = 0.15; // Service

    public Supplier selectBestVendor(List<SupplierMetrics> candidates) {
        System.out.println("\nVendorSelectionEngine: Evaluating suppliers based on formula: (Q*0.35) + (D*0.30) + (P*0.20) + (S*0.15)...");
        
        Supplier bestSupplier = null;
        double highestScore = -1.0;

        for (SupplierMetrics candidate : candidates) {
            // Execute the mathematical evaluation
            double score = (candidate.qualityScore * WQ) +
                           (candidate.deliveryScore * WD) +
                           (candidate.priceScore * WP) +
                           (candidate.serviceScore * WS);
            
            System.out.printf(" -> %s | Final Score: %.2f (Q:%.1f, D:%.1f, P:%.1f, S:%.1f)\n", 
                              candidate.supplier.getName(), score, 
                              candidate.qualityScore, candidate.deliveryScore, 
                              candidate.priceScore, candidate.serviceScore);

            if (score > highestScore) {
                highestScore = score;
                bestSupplier = candidate.supplier;
            }
        }

        System.out.println("VendorSelectionEngine: Best supplier selected -> " + bestSupplier.getName());
        return bestSupplier;
    }

    /**
     * Inner class to hold the 4 metrics sourced from historical data (QIR, Logs, Invoices).
     */
    public static class SupplierMetrics {
        public Supplier supplier;
        public double qualityScore;
        public double deliveryScore;
        public double priceScore;
        public double serviceScore;

        public SupplierMetrics(Supplier supplier, double q, double d, double p, double s) {
            this.supplier = supplier;
            this.qualityScore = q;
            this.deliveryScore = d;
            this.priceScore = p;
            this.serviceScore = s;
        }
    }
}