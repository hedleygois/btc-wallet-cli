package com.btcwallet.service;

import org.bitcoinj.core.Transaction;

/**
 * Calculates transaction fees based on network conditions.
 * Provides fee estimates for different priority levels.
 */
public class FeeCalculator {

    private final NetworkMonitor networkMonitor;

    /**
     * Fee priority levels.
     */
    public enum FeePriority {
        LOW,    // Slow confirmation, cheap fee
        MEDIUM, // Normal confirmation time
        HIGH,   // Fast confirmation, expensive fee
        CUSTOM  // Custom fee rate
    }

    /**
     * Creates a new FeeCalculator.
     *
     * @param networkMonitor Network monitor for checking network conditions
     */
    public FeeCalculator(NetworkMonitor networkMonitor) {
        this.networkMonitor = networkMonitor;
    }

    /**
     * Calculates fee for a transaction based on current network conditions.
     *
     * @param transaction BitcoinJ Transaction to calculate fee for
     * @return Fee in satoshis
     */
    public long calculateFee(org.bitcoinj.core.Transaction transaction) {
        return calculateFee(transaction, FeePriority.MEDIUM);
    }

    /**
     * Calculates fee for a transaction with specified priority.
     *
     * @param transaction BitcoinJ Transaction to calculate fee for
     * @param priority Fee priority level
     * @return Fee in satoshis
     */
    public long calculateFee(org.bitcoinj.core.Transaction transaction, FeePriority priority) {
        int feeRate = getFeeRate(priority);
        int transactionSize = transaction.bitcoinSerialize().length;
        
        return transactionSize * feeRate;
    }

    /**
     * Gets the current fee rate for a priority level.
     *
     * @param priority Fee priority level
     * @return Fee rate in satoshis per byte
     */
    public int getFeeRate(FeePriority priority) {
        // Adjust fee rates based on network congestion
        int mempoolSize = networkMonitor.getMempoolSize();
        int baseRate = getBaseFeeRate(priority);
        
        // Increase fees if network is congested
        if (mempoolSize > 5000) {
            baseRate = (int) (baseRate * 1.5); // 50% increase for congestion
        }
        
        if (mempoolSize > 10000) {
            baseRate = (int) (baseRate * 2.0); // 100% increase for high congestion
        }
        
        return baseRate;
    }

    /**
     * Gets base fee rate for a priority level (without network adjustments).
     *
     * @param priority Fee priority level
     * @return Base fee rate in satoshis per byte
     */
    private int getBaseFeeRate(FeePriority priority) {
        return switch (priority) {
            case LOW -> 1;    // ~1 sat/byte
            case MEDIUM -> 5; // ~5 sat/byte (default)
            case HIGH -> 20;  // ~20 sat/byte (fast confirmation)
            case CUSTOM -> 10; // Default custom rate
        };
    }

    /**
     * Calculates fee for a custom fee rate.
     *
     * @param transaction BitcoinJ Transaction to calculate fee for
     * @param satoshiPerByte Custom fee rate in satoshis per byte
     * @return Fee in satoshis
     */
    public long calculateCustomFee(org.bitcoinj.core.Transaction transaction, int satoshiPerByte) {
        int transactionSize = transaction.bitcoinSerialize().length;
        return transactionSize * satoshiPerByte;
    }

    /**
     * Estimates fee for a transaction of given size.
     *
     * @param transactionSize Estimated transaction size in bytes
     * @param priority Fee priority level
     * @return Estimated fee in satoshis
     */
    public long estimateFee(int transactionSize, FeePriority priority) {
        int feeRate = getFeeRate(priority);
        return transactionSize * feeRate;
    }

    /**
     * Gets fee estimate for different priority levels.
     *
     * @param transactionSize Estimated transaction size in bytes
     * @return Fee estimates for low, medium, and high priority
     */
    public FeeEstimate getFeeEstimates(int transactionSize) {
        long lowFee = estimateFee(transactionSize, FeePriority.LOW);
        long mediumFee = estimateFee(transactionSize, FeePriority.MEDIUM);
        long highFee = estimateFee(transactionSize, FeePriority.HIGH);
        
        return new FeeEstimate(lowFee, mediumFee, highFee);
    }

    /**
     * Fee estimate record for displaying multiple priority levels.
     */
    public record FeeEstimate(long low, long medium, long high) {
        public String toString() {
            return "Low: " + low + " sat, Medium: " + medium + " sat, High: " + high + " sat";
        }
    }

    /**
     * Converts satoshis to BTC.
     *
     * @param satoshis Amount in satoshis
     * @return Amount in BTC
     */
    public static double satoshisToBTC(long satoshis) {
        return satoshis / 100_000_000.0;
    }

    /**
     * Converts BTC to satoshis.
     *
     * @param btc Amount in BTC
     * @return Amount in satoshis
     */
    public static long btcToSatoshis(double btc) {
        return (long) (btc * 100_000_000);
    }
}