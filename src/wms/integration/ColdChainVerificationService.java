package wms.services.integration;

import wms.exceptions.WMSException;
import com.scm.subsystems.WarehouseMgmtSubsystem;

public class ColdChainVerificationService implements IColdChainVerificationService {

    private final WarehouseMgmtSubsystem exceptions = WarehouseMgmtSubsystem.INSTANCE;
    private static final double MAX_SAFE_TEMP = 4.0; // Celsius

    @Override
    public void verifyTemperatureHandoff(String sku, String dockId, double rfidTempReading, boolean transitBreachAlert) throws WMSException {
        System.out.println("ColdChainVerification: Checking temperature for " + sku + " at " + dockId);
        
        if (transitBreachAlert) {
            System.err.println("ColdChainVerification: Real-Time Delivery system reported a TEMPERATURE_THRESHOLD_BREACH during transit!");
            exceptions.onDamagedGoodsDetected(sku, dockId);
            throw new WMSException("Cold Chain Broken: Transit breach alert active for " + sku);
        }

        if (rfidTempReading > MAX_SAFE_TEMP) {
            System.err.println("ColdChainVerification: RFID tag sensor reports current temp is " + rfidTempReading + "C (Max safe is " + MAX_SAFE_TEMP + "C)");
            exceptions.onDamagedGoodsDetected(sku, dockId);
            throw new WMSException("Cold Chain Broken: Arrival temperature too high for " + sku);
        }

        System.out.println("ColdChainVerification: SUCCESS! Cold chain maintained perfectly.");
    }
}
