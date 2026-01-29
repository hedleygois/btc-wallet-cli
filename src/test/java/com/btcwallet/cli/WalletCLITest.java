package com.btcwallet.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import com.btcwallet.config.BitcoinConfig;
import com.btcwallet.service.BalanceService;
import com.btcwallet.service.BitcoinNodeClient;
import com.btcwallet.service.FeeCalculator;
import com.btcwallet.service.NetworkMonitor;
import com.btcwallet.service.TransactionService;
import com.btcwallet.service.WalletService;

class WalletCLITest {

    @Test
    void testCLIConstructorWithValidInput() {
        // Given
        WalletService walletService = new WalletService();

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> {
            NetworkMonitor networkMonitor = new NetworkMonitor();
            FeeCalculator feeCalculator = new FeeCalculator(networkMonitor);
            
            // Create mock BitcoinConfig and BitcoinNodeClient for testing
            BitcoinConfig mockConfig = mock(BitcoinConfig.class);
            when(mockConfig.isEnabled()).thenReturn(false); // Disable real node in tests
            BitcoinNodeClient bitcoinNodeClient = new BitcoinNodeClient(mockConfig);
            
            TransactionService transactionService = new TransactionService(
                walletService, feeCalculator, networkMonitor, bitcoinNodeClient);
            BalanceService balanceService = mock(BalanceService.class);
            WalletCLI cli = new WalletCLI(walletService, transactionService, feeCalculator, networkMonitor, balanceService);
            assertNotNull(cli);
        });
    }

    @Test
    void testCLIConstructorWithMockInput() {
        // Given
        WalletService walletService = new WalletService();
        NetworkMonitor networkMonitor = new NetworkMonitor();
        FeeCalculator feeCalculator = new FeeCalculator(networkMonitor);
        
        // Create mock BitcoinConfig and BitcoinNodeClient for testing
        BitcoinConfig mockConfig = mock(BitcoinConfig.class);
        when(mockConfig.isEnabled()).thenReturn(false); // Disable real node in tests
        BitcoinNodeClient bitcoinNodeClient = new BitcoinNodeClient(mockConfig);
        
        TransactionService transactionService = new TransactionService(
            walletService, feeCalculator, networkMonitor, bitcoinNodeClient);
        
        // Mock System.in with empty input
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("".getBytes()));
            
            // When/Then - should handle empty input gracefully
            assertDoesNotThrow(() -> {
                BalanceService balanceService = mock(BalanceService.class);
                WalletCLI cli = new WalletCLI(walletService, transactionService, feeCalculator, networkMonitor, balanceService);
                assertNotNull(cli);
            });
            
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void testScannerInputHandling() {
        // Given
        String testInput = "test command\n";
        Scanner scanner = new Scanner(testInput);

        // When/Then
        assertTrue(scanner.hasNextLine());
        assertEquals("test command", scanner.nextLine().trim());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    void testEmptyScannerHandling() {
        // Given
        Scanner scanner = new Scanner("");

        // When/Then
        assertFalse(scanner.hasNextLine());
        
        // Should not throw NoSuchElementException
        assertDoesNotThrow(() -> {
            scanner.close();
        });
    }

    @Test
    void testScannerClose() {
        // Given
        Scanner scanner = new Scanner("test");

        // When/Then
        assertDoesNotThrow(() -> {
            scanner.close();
        });
        
        // Trying to use closed scanner should not crash
        assertDoesNotThrow(() -> {
            try {
                scanner.hasNextLine();
            } catch (IllegalStateException e) {
                // Expected behavior for closed scanner
            }
        });
    }

    @Test
    void testFallbackInputMethod() {
        // Given
        Scanner scanner = new Scanner("test command");

        // When/Then - test the fallback logic
        String input = "";
        if (scanner.hasNextLine()) {
            input = scanner.nextLine();
        } else if (scanner.hasNext()) {
            input = scanner.next();
        }
        
        assertEquals("test command", input.trim());
        scanner.close();
    }

    @Test
    void testEmptyInputFallback() {
        // Given
        Scanner scanner = new Scanner("");

        // When/Then - test empty input handling
        String input = "";
        if (scanner.hasNextLine()) {
            input = scanner.nextLine();
        } else if (scanner.hasNext()) {
            input = scanner.next();
        }
        
        assertTrue(input.isEmpty());
        scanner.close();
    }

    @Test
    void testInputLoopBehavior() {
        // Given
        Scanner scanner = new Scanner("test\n");

        // When/Then - simulate the input loop behavior
        int iterations = 0;
        while (iterations < 3) { // Prevent infinite loop in test
            if (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();
                if (!input.isEmpty()) {
                    assertEquals("test", input);
                    break; // Found our input
                }
            } else {
                // Simulate the sleep behavior without actually sleeping
                break;
            }
            iterations++;
        }
        
        scanner.close();
    }
}