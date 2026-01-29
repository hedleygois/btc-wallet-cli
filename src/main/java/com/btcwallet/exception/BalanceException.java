package com.btcwallet.exception;

/**
 * Custom checked exception for balance-related operations.
 * This exception is thrown when there are issues with balance checking,
 * insufficient funds, or other balance-related problems.
 */
public class BalanceException extends Exception {
    
    /**
     * Creates a new BalanceException with the specified message.
     * 
     * @param message the detail message
     */
    public BalanceException(String message) {
        super(message);
    }
    
    /**
     * Creates a new BalanceException with the specified message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BalanceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Factory method for balance unavailable scenario.
     * 
     * @return BalanceException for balance unavailable
     */
    public static BalanceException balanceUnavailable() {
        return new BalanceException("Balance information unavailable");
    }
    
    /**
     * Factory method for insufficient funds scenario.
     * 
     * @return BalanceException for insufficient funds
     */
    public static BalanceException insufficientFunds() {
        return new BalanceException("Insufficient funds");
    }
    
    /**
     * Factory method for balance fetch failure.
     * 
     * @param details additional details about the failure
     * @return BalanceException for fetch failure
     */
    public static BalanceException balanceFetchFailed(String details) {
        return new BalanceException("Failed to fetch balance: " + details);
    }
    
    /**
     * Factory method for balance update failure.
     * 
     * @param details additional details about the failure
     * @return BalanceException for update failure
     */
    public static BalanceException balanceUpdateFailed(String details) {
        return new BalanceException("Failed to update balance: " + details);
    }
}