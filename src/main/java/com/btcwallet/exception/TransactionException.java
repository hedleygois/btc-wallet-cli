package com.btcwallet.exception;

/**
 * Exception for transaction-related errors.
 * Uses modern Java features like sealed classes and pattern matching.
 */
public sealed class TransactionException extends RuntimeException 
    permits TransactionException.InvalidTransactionException, 
            TransactionException.SigningException, 
            TransactionException.NetworkException, 
            TransactionException.FeeCalculationException {
    
    /**
     * Creates a new TransactionException.
     *
     * @param message Error message
     */
    public TransactionException(String message) {
        super(message);
    }
    
    /**
     * Creates a new TransactionException with cause.
     *
     * @param message Error message
     * @param cause Original exception cause
     */
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Invalid transaction data exception.
     */
    public static final class InvalidTransactionException extends TransactionException {
        public InvalidTransactionException(String message) {
            super("Invalid transaction: " + message);
        }
        
        public InvalidTransactionException(String message, Throwable cause) {
            super("Invalid transaction: " + message, cause);
        }
    }
    
    /**
     * Transaction signing exception.
     */
    public static final class SigningException extends TransactionException {
        public SigningException(String message) {
            super("Transaction signing failed: " + message);
        }
        
        public SigningException(String message, Throwable cause) {
            super("Transaction signing failed: " + message, cause);
        }
    }
    
    /**
     * Network-related transaction exception.
     */
    public static final class NetworkException extends TransactionException {
        public NetworkException(String message) {
            super("Network error during transaction: " + message);
        }
        
        public NetworkException(String message, Throwable cause) {
            super("Network error during transaction: " + message, cause);
        }
    }
    
    /**
     * Fee calculation exception.
     */
    public static final class FeeCalculationException extends TransactionException {
        public FeeCalculationException(String message) {
            super("Fee calculation failed: " + message);
        }
        
        public FeeCalculationException(String message, Throwable cause) {
            super("Fee calculation failed: " + message, cause);
        }
    }
    
    /**
     * Factory method for invalid transaction exceptions.
     */
    public static TransactionException invalidTransaction(String message) {
        return new InvalidTransactionException(message);
    }

    /**
     * Factory method for invalid transaction exceptions with cause.
     */
    public static TransactionException invalidTransaction(String message, Exception cause) {
        return new InvalidTransactionException(message, cause);
    }
    
    /**
     * Factory method for signing exceptions.
     */
    public static TransactionException signingFailed(String message) {
        return new SigningException(message);
    }

    /**
     * Factory method for signing exceptions with cause.
     */
    public static TransactionException signingFailed(String message, Exception cause) {
        return new SigningException(message, cause);
    }
    
    /**
     * Factory method for network exceptions.
     */
    public static TransactionException networkError(String message) {
        return new NetworkException(message);
    }

    /**
     * Factory method for network exceptions with cause.
     */
    public static TransactionException networkError(String message, Exception cause) {
        return new NetworkException(message, cause);
    }
    
    /**
     * Factory method for fee calculation exceptions.
     */
    public static TransactionException feeCalculationFailed(String message) {
        return new FeeCalculationException(message);
    }
    
    /**
     * Factory method for general transaction failures.
     */
    public static TransactionException transactionFailed(String message) {
        return new InvalidTransactionException(message);
    }
    
    /**
     * Factory method for general transaction failures with cause.
     */
    public static TransactionException transactionFailed(String message, Exception cause) {
        return new InvalidTransactionException(message, cause);
    }
    
    /**
     * Handles the exception using pattern matching (Java 21 preview feature).
     * This demonstrates modern Java exception handling.
     */
    public String handleWithPatternMatching() {
        return switch (this) {
            case InvalidTransactionException e -> "Invalid transaction detected: " + getMessage();
            case SigningException e -> "Signing failed: " + getMessage();
            case NetworkException e -> "Network issue: " + getMessage();
            case FeeCalculationException e -> "Fee calculation problem: " + getMessage();
            default -> "Transaction error: " + getMessage();
        };
    }
}