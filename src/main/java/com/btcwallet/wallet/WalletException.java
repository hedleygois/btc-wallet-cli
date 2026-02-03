package com.btcwallet.wallet;

/**
 * Base exception for wallet-related errors.
 * Provides human-readable error messages for better user experience.
 */
public class WalletException extends RuntimeException {
    
    private final ErrorType errorType;
    
    /**
     * Types of wallet errors for better categorization.
     */
    public enum ErrorType {
        /** Invalid input data (private key, mnemonic, etc.) */
        INVALID_INPUT,
        
        /** Network-related errors */
        NETWORK_ERROR,
        
        /** Wallet generation failures */
        GENERATION_ERROR,
        
        /** Wallet import failures */
        IMPORT_ERROR,
        
        /** Storage/IO related errors */
        STORAGE_ERROR,
        
        /** Security/validation errors */
        SECURITY_ERROR,
        
        /** Unknown/unspecified errors */
        UNKNOWN_ERROR
    }
    
    /**
     * Creates a new WalletException with a specific error type and message.
     *
     * @param errorType Type of error
     * @param message Human-readable error message
     */
    public WalletException(ErrorType errorType, String message) {
        super(createUserFriendlyMessage(errorType, message));
        this.errorType = errorType;
    }
    
    /**
     * Creates a new WalletException with a specific error type, message, and cause.
     *
     * @param errorType Type of error
     * @param message Human-readable error message
     * @param cause Original exception cause
     */
    public WalletException(ErrorType errorType, String message, Throwable cause) {
        super(createUserFriendlyMessage(errorType, message), cause);
        this.errorType = errorType;
    }
    
    /**
     * Creates a new WalletException from another exception with a specific error type.
     *
     * @param errorType Type of error
     * @param cause Original exception cause
     */
    public WalletException(ErrorType errorType, Throwable cause) {
        super(createUserFriendlyMessage(errorType, cause.getMessage()), cause);
        this.errorType = errorType;
    }
    
    /**
     * Gets the error type.
     *
     * @return Error type
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Creates a user-friendly error message based on error type and technical message.
     *
     * @param errorType Type of error
     * @param technicalMessage Technical error message
     * @return User-friendly error message
     */
    private static String createUserFriendlyMessage(ErrorType errorType, String technicalMessage) {
        String prefix = getErrorPrefix(errorType);
        String userMessage = getUserMessage(errorType, technicalMessage);
        
        return prefix + " " + userMessage + " (Technical details: " + technicalMessage + ")";
    }
    
    /**
     * Gets the error prefix based on error type.
     *
     * @param errorType Type of error
     * @return Error prefix
     */
    private static String getErrorPrefix(ErrorType errorType) {
        switch (errorType) {
            case INVALID_INPUT:
                return "[INPUT ERROR]";
            case NETWORK_ERROR:
                return "[NETWORK ERROR]";
            case GENERATION_ERROR:
                return "[GENERATION ERROR]";
            case IMPORT_ERROR:
                return "[IMPORT ERROR]";
            case STORAGE_ERROR:
                return "[STORAGE ERROR]";
            case SECURITY_ERROR:
                return "[SECURITY ERROR]";
            case UNKNOWN_ERROR:
            default:
                return "[WALLET ERROR]";
        }
    }
    
    /**
     * Gets a user-friendly message based on error type and technical details.
     *
     * @param errorType Type of error
     * @param technicalMessage Technical error message
     * @return User-friendly message
     */
    private static String getUserMessage(ErrorType errorType, String technicalMessage) {
        switch (errorType) {
            case INVALID_INPUT:
                if (technicalMessage != null && technicalMessage.toLowerCase().contains("private key")) {
                    return "The private key format is invalid. Please check that you've entered a valid hexadecimal or WIF private key.";
                } else if (technicalMessage != null && technicalMessage.toLowerCase().contains("mnemonic")) {
                    return "The seed phrase (mnemonic) is invalid. Please verify that all words are spelled correctly and in the right order.";
                } else {
                    return "The input data is invalid. Please check your input and try again.";
                }
                
            case IMPORT_ERROR:
                return "Failed to import wallet. The provided key or seed phrase may be invalid or corrupted.";
                
            case GENERATION_ERROR:
                return "Failed to generate new wallet. This may be due to a system error.";
                
            case NETWORK_ERROR:
                return "Network operation failed. Please check your internet connection.";
                
            case STORAGE_ERROR:
                return "Failed to save or load wallet data. Please check file permissions.";
                
            case SECURITY_ERROR:
                return "Security violation detected. Operation aborted for safety.";
                
            case UNKNOWN_ERROR:
            default:
                return "An unexpected error occurred while processing your wallet request.";
        }
    }
    
    /**
     * Creates a WalletException for invalid private key input.
     *
     * @param technicalMessage Technical error details
     * @return WalletException with appropriate error type and message
     */
    public static WalletException invalidPrivateKey(String technicalMessage) {
        return new WalletException(ErrorType.INVALID_INPUT, "Invalid private key: " + technicalMessage);
    }
    
    /**
     * Creates a WalletException for invalid mnemonic input.
     *
     * @param technicalMessage Technical error details
     * @return WalletException with appropriate error type and message
     */
    public static WalletException invalidMnemonic(String technicalMessage) {
        return new WalletException(ErrorType.INVALID_INPUT, "Invalid mnemonic: " + technicalMessage);
    }
    
    /**
     * Creates a WalletException for wallet import failures.
     *
     * @param technicalMessage Technical error details
     * @return WalletException with appropriate error type and message
     */
    public static WalletException importFailed(String technicalMessage) {
        return new WalletException(ErrorType.IMPORT_ERROR, "Wallet import failed: " + technicalMessage);
    }
    
    /**
     * Creates a WalletException for wallet generation failures.
     *
     * @param technicalMessage Technical error details
     * @return WalletException with appropriate error type and message
     */
    public static WalletException generationFailed(String technicalMessage) {
        return new WalletException(ErrorType.GENERATION_ERROR, "Wallet generation failed: " + technicalMessage);
    }

    /**
     * Creates a WalletException for balance-related errors.
     *
     * @param technicalMessage Technical error details
     * @return WalletException with appropriate error type and message
     */
    public static WalletException balanceError(String technicalMessage) {
        return new WalletException(ErrorType.NETWORK_ERROR, "Balance operation failed: " + technicalMessage);
    }

    /**
     * Creates a WalletException for balance-related errors with cause.
     *
     * @param technicalMessage Technical error details
     * @param cause Original exception cause
     * @return WalletException with appropriate error type and message
     */
    public static WalletException balanceError(String technicalMessage, Throwable cause) {
        return new WalletException(ErrorType.NETWORK_ERROR, "Balance operation failed: " + technicalMessage, cause);
    }
}