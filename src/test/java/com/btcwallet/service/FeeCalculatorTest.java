package com.btcwallet.service;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeCalculatorTest {

    @Mock
    private NetworkMonitor networkMonitor;

    private FeeCalculator feeCalculator;

    @BeforeEach
    void setUp() {
        feeCalculator = new FeeCalculator(networkMonitor);
    }

    @Test
    void testCalculateFee() {
        // Given
        Transaction transaction = new Transaction(MainNetParams.get());
        transaction.addOutput(org.bitcoinj.core.Coin.valueOf(100000), 
                            org.bitcoinj.core.Address.fromString(MainNetParams.get(), 
                            "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"));
        
        when(networkMonitor.getMempoolSize()).thenReturn(3000); // Normal congestion
        
        // When
        long fee = feeCalculator.calculateFee(transaction);
        
        // Then
        assertTrue(fee > 0);
        int transactionSize = transaction.bitcoinSerialize().length;
        int expectedFee = transactionSize * 5; // Medium priority = 5 sat/byte
        assertEquals(expectedFee, fee);
    }

    @Test
    void testCalculateFeeWithPriority() {
        // Given
        Transaction transaction = new Transaction(MainNetParams.get());
        transaction.addOutput(org.bitcoinj.core.Coin.valueOf(100000), 
                            org.bitcoinj.core.Address.fromString(MainNetParams.get(), 
                            "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"));
        
        int transactionSize = transaction.bitcoinSerialize().length;
        
        // Test LOW priority
        when(networkMonitor.getMempoolSize()).thenReturn(1000); // Low congestion
        long lowFee = feeCalculator.calculateFee(transaction, FeeCalculator.FeePriority.LOW);
        assertEquals(transactionSize * 1, lowFee); // 1 sat/byte
        
        // Test MEDIUM priority
        when(networkMonitor.getMempoolSize()).thenReturn(3000); // Normal congestion
        long mediumFee = feeCalculator.calculateFee(transaction, FeeCalculator.FeePriority.MEDIUM);
        assertEquals(transactionSize * 5, mediumFee); // 5 sat/byte
        
        // Test HIGH priority
        when(networkMonitor.getMempoolSize()).thenReturn(3000); // Normal congestion
        long highFee = feeCalculator.calculateFee(transaction, FeeCalculator.FeePriority.HIGH);
        assertEquals(transactionSize * 20, highFee); // 20 sat/byte
    }

    @Test
    void testFeeRateAdjustmentForCongestion() {
        // Test normal conditions
        when(networkMonitor.getMempoolSize()).thenReturn(2000);
        int normalRate = feeCalculator.getFeeRate(FeeCalculator.FeePriority.MEDIUM);
        assertEquals(5, normalRate);
        
        // Test moderate congestion
        when(networkMonitor.getMempoolSize()).thenReturn(6000);
        int moderateRate = feeCalculator.getFeeRate(FeeCalculator.FeePriority.MEDIUM);
        assertEquals(7, moderateRate); // 5 * 1.5 = 7.5, cast to int = 7
        
        // Test high congestion
        when(networkMonitor.getMempoolSize()).thenReturn(12000);
        int highRate = feeCalculator.getFeeRate(FeeCalculator.FeePriority.MEDIUM);
        assertEquals(14, highRate); // 5 * 1.5 = 7.5 → 7, then 7 * 2 = 14
    }

    @Test
    void testCustomFeeCalculation() {
        // Given
        Transaction transaction = new Transaction(MainNetParams.get());
        transaction.addOutput(org.bitcoinj.core.Coin.valueOf(100000), 
                            org.bitcoinj.core.Address.fromString(MainNetParams.get(), 
                            "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"));
        
        int customRate = 15; // 15 sat/byte
        
        // When
        long fee = feeCalculator.calculateCustomFee(transaction, customRate);
        
        // Then
        int transactionSize = transaction.bitcoinSerialize().length;
        assertEquals(transactionSize * customRate, fee);
    }

    @Test
    void testFeeEstimation() {
        // Given
        int transactionSize = 226; // Typical transaction size
        when(networkMonitor.getMempoolSize()).thenReturn(3000); // Normal congestion
        
        // When
        FeeCalculator.FeeEstimate estimates = feeCalculator.getFeeEstimates(transactionSize);
        
        // Then
        assertEquals(transactionSize * 1, estimates.low());
        assertEquals(transactionSize * 5, estimates.medium());
        assertEquals(transactionSize * 20, estimates.high());
    }

    @Test
    void testSatoshiConversions() {
        // Test BTC to satoshis
        assertEquals(100000000L, FeeCalculator.btcToSatoshis(1.0));
        assertEquals(50000000L, FeeCalculator.btcToSatoshis(0.5));
        assertEquals(100000L, FeeCalculator.btcToSatoshis(0.001));
        assertEquals(5000L, FeeCalculator.btcToSatoshis(0.00005));
        
        // Test satoshis to BTC
        assertEquals(1.0, FeeCalculator.satoshisToBTC(100000000L), 0.000001);
        assertEquals(0.5, FeeCalculator.satoshisToBTC(50000000L), 0.000001);
        assertEquals(0.001, FeeCalculator.satoshisToBTC(100000L), 0.000001);
        assertEquals(0.00005, FeeCalculator.satoshisToBTC(5000L), 0.000001);
    }

    @Test
    void testFeeEstimateToString() {
        // Given
        FeeCalculator.FeeEstimate estimate = new FeeCalculator.FeeEstimate(226, 1130, 4520);
        
        // When
        String result = estimate.toString();
        
        // Then
        assertTrue(result.contains("226"));
        assertTrue(result.contains("1130"));
        assertTrue(result.contains("4520"));
    }
}