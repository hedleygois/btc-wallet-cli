package com.btcwallet.exception;

/**
 * Custom checked exception for Bitcoin configuration errors.
 * This exception is thrown when there are issues loading or validating
 * Bitcoin node configuration.
 */
public class BitcoinConfigurationException extends Exception {
    
    /**
     * Creates a new BitcoinConfigurationException with the specified message.
     * 
     * @param message the detail message
     */
    public BitcoinConfigurationException(String message) {
        super(message);
    }
    
    /**
     * Creates a new BitcoinConfigurationException with the specified message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BitcoinConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}