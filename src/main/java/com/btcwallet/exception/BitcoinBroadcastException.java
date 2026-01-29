package com.btcwallet.exception;

/**
 * Custom checked exception for Bitcoin broadcast failures.
 * This exception is thrown when there are issues broadcasting transactions
 * to the Bitcoin network.
 */
public class BitcoinBroadcastException extends Exception {
    
    /**
     * Creates a new BitcoinBroadcastException with the specified message.
     * 
     * @param message the detail message
     */
    public BitcoinBroadcastException(String message) {
        super(message);
    }
    
    /**
     * Creates a new BitcoinBroadcastException with the specified message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BitcoinBroadcastException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Factory method for network unavailable scenario.
     * 
     * @return BitcoinBroadcastException for network unavailable
     */
    public static BitcoinBroadcastException networkUnavailable() {
        return new BitcoinBroadcastException("Bitcoin network unavailable");
    }
    
    /**
     * Factory method for network congestion scenario.
     * 
     * @return BitcoinBroadcastException for network congestion
     */
    public static BitcoinBroadcastException networkCongestion() {
        return new BitcoinBroadcastException("Network congestion high, try again later");
    }
    
    /**
     * Factory method for general broadcast failure.
     * 
     * @param details additional details about the failure
     * @return BitcoinBroadcastException for broadcast failure
     */
    public static BitcoinBroadcastException broadcastFailed(String details) {
        return new BitcoinBroadcastException("Failed to broadcast transaction: " + details);
    }
}