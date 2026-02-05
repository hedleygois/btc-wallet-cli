package com.btcwallet.transaction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import org.bitcoinj.params.MainNetParams;

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

    public enum TransactionStatus {
        PENDING,        // Transaction created but not signed
        SIGNED,         // Transaction signed but not broadcasted
        BROADCASTED,    // Transaction sent to network
        CONFIRMING,     // Transaction in mempool, awaiting confirmations
        CONFIRMED,      // Transaction confirmed in block
        FAILED,         // Transaction failed
        SIMULATED       // Simulation completed (not broadcasted)
    }

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
        var transactionHash = rawTransaction.getTxId().toString();
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

    public long getTotalAmount() {
        return amount + fee;
    }

    public BigDecimal getAmountAsBigDecimal() {
        return new BigDecimal(org.bitcoinj.core.Coin.valueOf(amount).toPlainString());
    }

    public BigDecimal getFeeAsBigDecimal() {
        return new BigDecimal(org.bitcoinj.core.Coin.valueOf(fee).toPlainString());
    }

    public String getAmountInBTC() {
        return org.bitcoinj.core.Coin.valueOf(amount).toFriendlyString();
    }

    public String getFeeInBTC() {
        return org.bitcoinj.core.Coin.valueOf(fee).toFriendlyString();
    }

    public boolean isConfirmed() {
        return status == TransactionStatus.CONFIRMED;
    }

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