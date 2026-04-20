package wms.integration;

import wms.exceptions.WMSException;

/**
 * Verifies cold chain integrity using RFID temperature sensors and Real-Time Delivery alerts.
 */
public interface IColdChainVerificationService {
    /**
     * Cross-references RFID dock scans with Real-Time transit data.
     * @param sku The perishable product SKU.
     * @param dockId The receiving dock.
     * @param rfidTempReading The temperature recorded by the RFID tag upon arrival.
     * @param transitBreachAlert True if Real-Time system reported a breach during transit.
     * @throws WMSException if the cold chain is broken.
     */
    void verifyTemperatureHandoff(String sku, String dockId, double rfidTempReading, boolean transitBreachAlert) throws WMSException;
}
