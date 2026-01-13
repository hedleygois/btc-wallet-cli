package com.btcwallet.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalletExceptionTest {

    @Test
    void testWalletExceptionCreation() {
        // Test basic exception creation
        WalletException exception = new WalletException(
            WalletException.ErrorType.INVALID_INPUT, 
            "Test error message"
        );
        
        assertNotNull(exception);
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("Test error message"));
        assertTrue(exception.getMessage().contains("Technical details:"));
    }

    @Test
    void testWalletExceptionWithCause() {
        // Test exception with cause
        Exception cause = new RuntimeException("Original cause");
        WalletException exception = new WalletException(
            WalletException.ErrorType.NETWORK_ERROR, 
            "Network failed", 
            cause
        );
        
        assertNotNull(exception);
        assertEquals(WalletException.ErrorType.NETWORK_ERROR, exception.getErrorType());
        assertEquals(cause, exception.getCause());
        assertTrue(exception.getMessage().contains("[NETWORK ERROR]"));
    }

    @Test
    void testWalletExceptionFromCause() {
        // Test exception created from cause
        Exception cause = new RuntimeException("Original error");
        WalletException exception = new WalletException(
            WalletException.ErrorType.GENERATION_ERROR, 
            cause
        );
        
        assertNotNull(exception);
        assertEquals(WalletException.ErrorType.GENERATION_ERROR, exception.getErrorType());
        assertEquals(cause, exception.getCause());
        assertTrue(exception.getMessage().contains("[GENERATION ERROR]"));
    }

    @Test
    void testInvalidPrivateKeyException() {
        // Test convenience method for invalid private key
        WalletException exception = WalletException.invalidPrivateKey("Hex format error");
        
        assertNotNull(exception);
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("private key"));
        assertTrue(exception.getMessage().contains("Hex format error"));
    }

    @Test
    void testInvalidMnemonicException() {
        // Test convenience method for invalid mnemonic
        WalletException exception = WalletException.invalidMnemonic("Word not in BIP39 list");
        
        assertNotNull(exception);
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("mnemonic"));
        assertTrue(exception.getMessage().contains("Word not in BIP39 list"));
    }

    @Test
    void testImportFailedException() {
        // Test convenience method for import failure
        WalletException exception = WalletException.importFailed("Corrupted wallet data");
        
        assertNotNull(exception);
        assertEquals(WalletException.ErrorType.IMPORT_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[IMPORT ERROR]"));
        assertTrue(exception.getMessage().contains("Corrupted wallet data"));
    }

    @Test
    void testGenerationFailedException() {
        // Test convenience method for generation failure
        WalletException exception = WalletException.generationFailed("Entropy generation failed");
        
        assertNotNull(exception);
        assertEquals(WalletException.ErrorType.GENERATION_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[GENERATION ERROR]"));
        assertTrue(exception.getMessage().contains("Entropy generation failed"));
    }

    @Test
    void testErrorTypeMessages() {
        // Test that different error types produce appropriate user messages
        
        // Test INVALID_INPUT with private key
        WalletException privateKeyException = WalletException.invalidPrivateKey("Invalid hex");
        assertTrue(privateKeyException.getMessage().contains("private key"));
        assertTrue(privateKeyException.getMessage().contains("hexadecimal"));
        
        // Test INVALID_INPUT with mnemonic
        WalletException mnemonicException = WalletException.invalidMnemonic("Invalid word");
        assertTrue(mnemonicException.getMessage().contains("seed phrase"));
        assertTrue(mnemonicException.getMessage().contains("mnemonic"));
        
        // Test IMPORT_ERROR
        WalletException importException = WalletException.importFailed("Corrupted data");
        assertTrue(importException.getMessage().contains("Failed to import wallet"));
        
        // Test GENERATION_ERROR
        WalletException generationException = WalletException.generationFailed("System error");
        assertTrue(generationException.getMessage().contains("Failed to generate new wallet"));
        
        // Test NETWORK_ERROR
        WalletException networkException = new WalletException(
            WalletException.ErrorType.NETWORK_ERROR, 
            "Connection timeout"
        );
        assertTrue(networkException.getMessage().contains("Network operation failed"));
        
        // Test STORAGE_ERROR
        WalletException storageException = new WalletException(
            WalletException.ErrorType.STORAGE_ERROR, 
            "File not found"
        );
        assertTrue(storageException.getMessage().contains("Failed to save or load wallet data"));
        
        // Test SECURITY_ERROR
        WalletException securityException = new WalletException(
            WalletException.ErrorType.SECURITY_ERROR, 
            "Permission denied"
        );
        assertTrue(securityException.getMessage().contains("Security violation detected"));
        
        // Test UNKNOWN_ERROR
        WalletException unknownException = new WalletException(
            WalletException.ErrorType.UNKNOWN_ERROR, 
            "Unknown error"
        );
        assertTrue(unknownException.getMessage().contains("unexpected error"));
    }

    @Test
    void testExceptionMessageFormat() {
        // Test that exception messages follow the expected format
        WalletException exception = new WalletException(
            WalletException.ErrorType.INVALID_INPUT, 
            "Test technical message"
        );
        
        String message = exception.getMessage();
        
        // Should contain prefix
        assertTrue(message.startsWith("[INPUT ERROR]"));
        
        // Should contain user-friendly message
        assertTrue(message.contains("input data is invalid"));
        
        // Should contain technical details
        assertTrue(message.contains("Technical details:"));
        assertTrue(message.contains("Test technical message"));
    }

    @Test
    void testExceptionInheritance() {
        // Test that WalletException is properly a RuntimeException
        WalletException exception = new WalletException(
            WalletException.ErrorType.UNKNOWN_ERROR, 
            "Test"
        );
        
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }
}