package com.btcwallet.model;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Coin;
import org.bitcoinj.params.MainNetParams;
import org.junit.jupiter.api.Test;

import com.btcwallet.transaction.Transaction;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private final NetworkParameters mainNet = MainNetParams.get();
    private final String testWalletId = "WALLET-TEST-001";
    private final String testRecipient = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";

    @Test
    void testTransactionCreation() {
        // Given
        String transactionId = "tx-001";
        long amount = 100000; // 0.001 BTC
        long fee = 5000;     // 0.00005 BTC
        Instant createdAt = Instant.now();
        
        // When
        com.btcwallet.transaction.Transaction transaction = new com.btcwallet.transaction.Transaction(
            transactionId, testWalletId, testRecipient, amount, fee,
            com.btcwallet.transaction.Transaction.TransactionStatus.PENDING, createdAt, true, null
        );
        
        // Then
        assertEquals(transactionId, transaction.transactionId());
        assertEquals(testWalletId, transaction.walletId());
        assertEquals(testRecipient, transaction.recipientAddress());
        assertEquals(amount, transaction.amount());
        assertEquals(fee, transaction.fee());
        assertEquals(Transaction.TransactionStatus.PENDING, transaction.status());
        assertEquals(createdAt, transaction.createdAt());
        assertTrue(transaction.isSimulation());
        assertNull(transaction.rawTransaction());
    }

    @Test
    void testTransactionValidation() {
        // Test null transaction ID
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction(null, testWalletId, testRecipient, 100000, 5000,
                          Transaction.TransactionStatus.PENDING, Instant.now(), true, null);
        });
        
        // Test empty transaction ID
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("", testWalletId, testRecipient, 100000, 5000,
                          Transaction.TransactionStatus.PENDING, Instant.now(), true, null);
        });
        
        // Test null wallet ID
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("tx-001", null, testRecipient, 100000, 5000,
                          Transaction.TransactionStatus.PENDING, Instant.now(), true, null);
        });
        
        // Test invalid recipient address
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("tx-001", testWalletId, "", 100000, 5000,
                          Transaction.TransactionStatus.PENDING, Instant.now(), true, null);
        });
        
        // Test zero amount
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("tx-001", testWalletId, testRecipient, 0, 5000,
                          Transaction.TransactionStatus.PENDING, Instant.now(), true, null);
        });
        
        // Test negative amount
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("tx-001", testWalletId, testRecipient, -100, 5000,
                          Transaction.TransactionStatus.PENDING, Instant.now(), true, null);
        });
        
        // Test negative fee
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("tx-001", testWalletId, testRecipient, 100000, -1,
                          Transaction.TransactionStatus.PENDING, Instant.now(), true, null);
        });
    }

    @Test
    void testTransactionStatuses() {
        // Test all transaction statuses
        for (Transaction.TransactionStatus status : Transaction.TransactionStatus.values()) {
            Transaction transaction = new Transaction(
                "tx-test", testWalletId, testRecipient, 100000, 5000,
                status, Instant.now(), true, null
            );
            
            assertEquals(status, transaction.status());
        }
    }

    @Test
    void testTransactionAmountCalculations() {
        // Given
        long amount = 100000; // 0.001 BTC
        long fee = 5000;     // 0.00005 BTC
        Transaction transaction = new Transaction(
            "tx-001", testWalletId, testRecipient, amount, fee,
            Transaction.TransactionStatus.SIGNED, Instant.now(), false, null
        );
        
        // When/Then
        assertEquals(105000, transaction.getTotalAmount());
        assertEquals("0.001 BTC", transaction.getAmountInBTC());
        assertTrue(transaction.getFeeInBTC().contains("0.00005 BTC"));
    }

    @Test
    void testTransactionStatusMethods() {
        // Test confirmed transaction
        Transaction confirmed = new Transaction(
            "tx-001", testWalletId, testRecipient, 100000, 5000,
            Transaction.TransactionStatus.CONFIRMED, Instant.now(), false, null
        );
        assertTrue(confirmed.isConfirmed());
        assertFalse(confirmed.isFailed());
        
        // Test failed transaction
        Transaction failed = new Transaction(
            "tx-002", testWalletId, testRecipient, 100000, 5000,
            Transaction.TransactionStatus.FAILED, Instant.now(), false, null
        );
        assertFalse(failed.isConfirmed());
        assertTrue(failed.isFailed());
        
        // Test pending transaction
        Transaction pending = new Transaction(
            "tx-003", testWalletId, testRecipient, 100000, 5000,
            Transaction.TransactionStatus.PENDING, Instant.now(), false, null
        );
        assertFalse(pending.isConfirmed());
        assertFalse(pending.isFailed());
    }

    @Test
    void testTransactionFromBitcoinJ() {
        // Given
        ECKey ecKey = new ECKey();
        org.bitcoinj.core.Transaction bitcoinJTransaction = new org.bitcoinj.core.Transaction(mainNet);
        
        // Add output - simplified for testing
        org.bitcoinj.core.Address address = org.bitcoinj.core.Address.fromString(mainNet, "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa");
        bitcoinJTransaction.addOutput(Coin.valueOf(100000), address);
        
        // When
        com.btcwallet.transaction.Transaction transaction = com.btcwallet.transaction.Transaction.fromTransaction(
            testWalletId, bitcoinJTransaction, true, 5000L
        );
        
        // Then
        assertEquals(testWalletId, transaction.walletId());
        assertEquals(Transaction.TransactionStatus.SIMULATED, transaction.status());
        assertTrue(transaction.isSimulation());
        assertNotNull(transaction.rawTransaction());
        assertEquals(100000, transaction.amount());
        assertTrue(transaction.fee() > 0); // Fee should be calculated
    }

    @Test
    void testTransactionEquality() {
        // Given
        Instant createdAt = Instant.now();
        Transaction tx1 = new Transaction(
            "tx-001", testWalletId, testRecipient, 100000, 5000,
            Transaction.TransactionStatus.SIGNED, createdAt, false, null
        );
        
        Transaction tx2 = new Transaction(
            "tx-001", testWalletId, testRecipient, 100000, 5000,
            Transaction.TransactionStatus.SIGNED, createdAt, false, null
        );
        
        Transaction tx3 = new Transaction(
            "tx-002", testWalletId, testRecipient, 100000, 5000,
            Transaction.TransactionStatus.SIGNED, createdAt, false, null
        );
        
        // When/Then
        assertEquals(tx1, tx2); // Same transaction ID should be equal
        assertNotEquals(tx1, tx3); // Different transaction IDs should not be equal
        assertEquals(tx1.hashCode(), tx2.hashCode());
        assertNotEquals(tx1.hashCode(), tx3.hashCode());
    }

    @Test
    void testTransactionToString() {
        // Given
        Transaction transaction = new Transaction(
            "tx-001", testWalletId, testRecipient, 100000, 5000,
            Transaction.TransactionStatus.SIGNED, Instant.now(), false, null
        );
        
        // When
        String toString = transaction.toString();
        
        // Then
        assertTrue(toString.contains("tx-001"));
        assertTrue(toString.contains(testWalletId));
        assertTrue(toString.contains(testRecipient));
        assertTrue(toString.contains("0.001 BTC"));
        assertTrue(toString.contains("0.00005 BTC"));
        assertTrue(toString.contains("SIGNED"));
    }
}