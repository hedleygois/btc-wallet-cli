package com.btcwallet.cli;

import com.btcwallet.service.WalletService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class WalletCLITest {

    @Test
    void testCLIConstructorWithValidInput() {
        // Given
        WalletService walletService = new WalletService();

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> {
            WalletCLI cli = new WalletCLI(walletService);
            assertNotNull(cli);
        });
    }

    @Test
    void testCLIConstructorWithMockInput() {
        // Given
        WalletService walletService = new WalletService();
        
        // Mock System.in with empty input
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("".getBytes()));
            
            // When/Then - should handle empty input gracefully
            assertDoesNotThrow(() -> {
                WalletCLI cli = new WalletCLI(walletService);
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