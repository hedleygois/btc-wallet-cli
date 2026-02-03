package com.btcwallet.transaction;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.params.MainNetParams;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a Bitcoin transaction.
 *
 * @param transactionId Unique transaction hash/ID
 * @param walletId Wallet ID that created this transaction
 * @param recipientAddress Bitcoin address of the recipient
 * @param amount Transaction amount in satoshis
 * @param fee Transaction fee in satoshis
 * @param status Current transaction status
 * @param createdAt Timestamp when transaction was created
 * @param isSimulation Whether this is a simulation (not broadcasted)
 * @param rawTransaction Raw BitcoinJ transaction object
 */
public record Transaction(
    String transactionId,
    String walletId,
    String recipientAddress,
    long amount,
    long fee,
    TransactionStatus status,
    Instant createdAt,
    boolean isSimulation,
    org.bitcoinj.core.Transaction rawTransaction
) implements Serializable {

    /**
     * Transaction status enum.
     */
    public enum TransactionStatus {
        PENDING,        // Transaction created but not signed
        SIGNED,         // Transaction signed but not broadcasted
        BROADCASTED,    // Transaction sent to network
        CONFIRMING,     // Transaction in mempool, awaiting confirmations
        CONFIRMED,      // Transaction confirmed in block
        FAILED,         // Transaction failed
        SIMULATED       // Simulation completed (not broadcasted)
    }

    /**
     * Compact constructor for validation and default values.
     */
    public Transaction {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        if (walletId == null || walletId.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet ID cannot be null or empty");
        }
        if (recipientAddress == null || recipientAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient address cannot be null or empty");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (fee < 0) {
            throw new IllegalArgumentException("Fee cannot be negative");
        }
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Creates a Transaction from a BitcoinJ Transaction object.
     *
     * @param walletId Wallet ID
     * @param rawTransaction BitcoinJ transaction
     * @param isSimulation Whether this is a simulation
     * @return New Transaction instance
     */
    public static Transaction fromTransaction(String walletId, org.bitcoinj.core.Transaction rawTransaction, boolean isSimulation, long fee) {
        var transactionHash = rawTransaction.getHashAsString();
        var output = rawTransaction.getOutput(0);
        var recipientAddress = output.getScriptPubKey().getToAddress(MainNetParams.get()).toString();
        var amount = output.getValue().getValue();
        // Use the provided fee instead of calculating it
        // This allows the fee to be properly mocked in tests
        
        return new Transaction(
            transactionHash,
            walletId,
            recipientAddress,
            amount,
            fee,
            isSimulation ? TransactionStatus.SIMULATED : TransactionStatus.SIGNED,
            Instant.now(),
            isSimulation,
            rawTransaction
        );
    }

    /**
     * Calculates estimated transaction fee.
     * Simplified version - real implementation would use network conditions.
     */
    private static long calculateEstimatedFee(org.bitcoinj.core.Transaction transaction) {
        // Basic fee calculation: size * satoshi-per-byte
        byte[] serialized = transaction.bitcoinSerialize();
        int size = serialized.length;
        int satoshiPerByte = 5; // Default fee rate
        return size * satoshiPerByte;
    }

    /**
     * Gets the total amount including fee.
     */
    public long getTotalAmount() {
        return amount + fee;
    }

    /**
     * Gets the amount in BTC (not satoshis).
     */
    public String getAmountInBTC() {
        return Coin.valueOf(amount).toFriendlyString();
    }

    /**
     * Gets the fee in BTC (not satoshis).
     */
    public String getFeeInBTC() {
        return Coin.valueOf(fee).toFriendlyString();
    }

    /**
     * Checks if transaction is confirmed.
     */
    public boolean isConfirmed() {
        return status == TransactionStatus.CONFIRMED;
    }

    /**
     * Checks if transaction failed.
     */
    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }

    @Override
    public String toString() {
        return "Transaction[" +
                "id='" + transactionId + '\'' +
                ", wallet='" + walletId + '\'' +
                ", recipient='" + recipientAddress + '\'' +
                ", amount=" + getAmountInBTC() + " BTC" +
                ", fee=" + getFeeInBTC() + " BTC" +
                ", status=" + status +
                ", created=" + createdAt +
                ", simulation=" + isSimulation +
                ']';
    }
}