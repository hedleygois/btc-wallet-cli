package com.btcwallet.transaction;

/**
 * Exception for transaction-related errors.
 */
public sealed class TransactionException extends RuntimeException 
    permits TransactionException.InvalidTransactionException, 
            TransactionException.SigningException, 
            TransactionException.NetworkException, 
            TransactionException.FeeCalculationException {
    
    public TransactionException(String message) {
        super(message);
    }
    
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static final class InvalidTransactionException extends TransactionException {
        public InvalidTransactionException(String message) {
            super("Invalid transaction: " + message);
        }
        
        public InvalidTransactionException(String message, Throwable cause) {
            super("Invalid transaction: " + message, cause);
        }
    }
    
    public static final class SigningException extends TransactionException {
        public SigningException(String message) {
            super("Transaction signing failed: " + message);
        }
        
        public SigningException(String message, Throwable cause) {
            super("Transaction signing failed: " + message, cause);
        }
    }
    
    public static final class NetworkException extends TransactionException {
        public NetworkException(String message) {
            super("Network error during transaction: " + message);
        }
        
        public NetworkException(String message, Throwable cause) {
            super("Network error during transaction: " + message, cause);
        }
    }
    
    public static final class FeeCalculationException extends TransactionException {
        public FeeCalculationException(String message) {
            super("Fee calculation failed: " + message);
        }
        
        public FeeCalculationException(String message, Throwable cause) {
            super("Fee calculation failed: " + message, cause);
        }
    }
    
    public static TransactionException invalidTransaction(String message) {
        return new InvalidTransactionException(message);
    }

    public static TransactionException invalidTransaction(String message, Exception cause) {
        return new InvalidTransactionException(message, cause);
    }
    
    public static TransactionException signingFailed(String message) {
        return new SigningException(message);
    }

    public static TransactionException signingFailed(String message, Exception cause) {
        return new SigningException(message, cause);
    }
    
    public static TransactionException networkError(String message) {
        return new NetworkException(message);
    }

    public static TransactionException networkError(String message, Exception cause) {
        return new NetworkException(message, cause);
    }
    
    public static TransactionException feeCalculationFailed(String message) {
        return new FeeCalculationException(message);
    }
    
    public static TransactionException transactionFailed(String message) {
        return new InvalidTransactionException(message);
    }
    
    public static TransactionException transactionFailed(String message, Exception cause) {
        return new InvalidTransactionException(message, cause);
    }
    
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