package com.btcwallet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.btcwallet.network.FeeCalculator;
import com.btcwallet.network.NetworkMonitor;

import java.time.LocalTime;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class NetworkMonitorTest {

    private NetworkMonitor networkMonitor;
    private Random mockRandom;

    @BeforeEach
    void setUp() {
        networkMonitor = new NetworkMonitor();
        mockRandom = Mockito.mock(Random.class);
        
        try {
            java.lang.reflect.Field randomField = NetworkMonitor.class.getDeclaredField("random");
            randomField.setAccessible(true);
            randomField.set(networkMonitor, mockRandom);
        } catch (Exception e) {
            fail("Failed to mock random field: " + e.getMessage());
        }
    }

    @Test
    void testNetworkAvailability() {
        // Mock random.nextDouble() to return 0.1 (> 0.05)
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.1);
        
        boolean available = networkMonitor.isNetworkAvailable();
        assertTrue(available, "Network should be available when random > 0.05");
    }

    @Test
    void testMempoolSize() {
        // Mock random.nextInt()
        Mockito.when(mockRandom.nextInt(any(Integer.class))).thenReturn(2000);
        
        int mempoolSize = networkMonitor.getMempoolSize();
        // Depending on the hour, it will be 3000+2000 or 1000+2000
        assertTrue(mempoolSize == 5000 || mempoolSize == 3000, "Mempool size should be 3000 or 5000");
    }

    @Test
    void testNetworkHashRate() {
        // Hash rate should be in reasonable range for Bitcoin network
        long hashRate = networkMonitor.getNetworkHashRate();
        assertTrue(hashRate >= 350_000_000, "Hash rate should be at least 350 TH/s");
        assertTrue(hashRate <= 500_000_000, "Hash rate should be at most 500 TH/s");
    }

    @Test
    void testAverageBlockTime() {
        // Block time should be around 10 minutes (600 seconds)
        int blockTime = networkMonitor.getAverageBlockTime();
        assertTrue(blockTime >= 500, "Block time should be at least 500 seconds");
        assertTrue(blockTime <= 700, "Block time should be at most 700 seconds");
    }

    @Test
    void testCurrentDifficulty() {
        // Difficulty should be in reasonable range
        long difficulty = networkMonitor.getCurrentDifficulty();
        assertTrue(difficulty > 0, "Difficulty should be positive");
    }

    @Test
    void testCongestionLevel() {
        // Congestion level should be between 0.0 and 1.0
        double congestion = networkMonitor.getCongestionLevel();
        assertTrue(congestion >= 0.0, "Congestion should be at least 0.0");
        assertTrue(congestion <= 1.0, "Congestion should be at most 1.0");
    }

    @Test
    void testNetworkHealth() {
        // Mock LocalTime.now() to return 10 (business hours)
        // Mock random.nextDouble() to return 0.1 (> 0.05, so network is available)
        // This should result in:
        // - getMempoolSize() returns 3000-10000 (business hours)
        // - getCongestionLevel() returns 0.8 (congested)
        // - isNetworkAvailable() returns true (0.1 > 0.05)
        // - isNetworkHealthy() should return false (congestion >= 0.8)
        
        try (MockedStatic<LocalTime> mockedLocalTime = Mockito.mockStatic(LocalTime.class)) {
            LocalTime mockTime = Mockito.mock(LocalTime.class);
            mockedLocalTime.when(LocalTime::now).thenReturn(mockTime);
            Mockito.when(mockTime.getHour()).thenReturn(10);
            
            // Mock random.nextDouble() to return 0.1 (network available since 0.1 > 0.05)
            Mockito.when(mockRandom.nextDouble()).thenReturn(0.1);
            
            // Mock getMempoolSize() to return a value that gives congestion level 0.8
            // For congestion level 0.8, mempool size should be between 8000-12000
            Mockito.when(mockRandom.nextInt(7000)).thenReturn(5000); // 3000 + 5000 = 8000
            
            boolean healthy = networkMonitor.isNetworkHealthy();
            
            // With congestion level 0.8, network should NOT be healthy
            assertFalse(healthy, "Network should not be healthy when congestion level is 0.8");
            
            // Verify the conditions
            int mempoolSize = networkMonitor.getMempoolSize();
            assertEquals(8000, mempoolSize, "Mempool size should be 8000");
            
            double congestion = networkMonitor.getCongestionLevel();
            assertEquals(0.8, congestion, "Congestion level should be 0.8");
            
            boolean available = networkMonitor.isNetworkAvailable();
            assertTrue(available, "Network should be available when random > 0.05");
        }
    }

    @Test
    void testNetworkStatus() {
        // Network status should contain expected information
        String status = networkMonitor.getNetworkStatus();
        assertTrue(status.contains("Network:"), "Status should contain network info");
        assertTrue(status.contains("Mempool:"), "Status should contain mempool info");
        assertTrue(status.contains("Congestion:"), "Status should contain congestion info");
    }

    @Test
    void testFeeRecommendation() {
        // Fee recommendation should be one of the valid priorities
        FeeCalculator.FeePriority recommendation = networkMonitor.getFeeRecommendation();
        assertNotNull(recommendation, "Recommendation should not be null");
        assertTrue(recommendation == FeeCalculator.FeePriority.LOW ||
                  recommendation == FeeCalculator.FeePriority.MEDIUM ||
                  recommendation == FeeCalculator.FeePriority.HIGH,
                  "Recommendation should be a valid priority");
    }

    @Test
    void testNetworkConnection() {
        // Mock random.nextDouble() to return 0.1 (> 0.05, so network is available when networkAvailable is true)
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.1);

        // Test connecting to network
        networkMonitor.connectToNetwork();
        assertTrue(networkMonitor.isNetworkAvailable(), "Network should be available after connecting");
        
        // Test disconnecting from network
        networkMonitor.disconnectFromNetwork();
        assertFalse(networkMonitor.isNetworkAvailable(), "Network should be unavailable after disconnecting");
    }

    @Test
    void testMempoolSizeVariation() {
        // Test that mempool size varies based on time of day
        int mempool1 = networkMonitor.getMempoolSize();
        
        // Wait a bit and check again (simulated)
        int mempool2 = networkMonitor.getMempoolSize();
        
        // Mempool sizes should be reasonable
        assertTrue(mempool1 >= 1000 && mempool1 <= 15000);
        assertTrue(mempool2 >= 1000 && mempool2 <= 15000);
    }
}