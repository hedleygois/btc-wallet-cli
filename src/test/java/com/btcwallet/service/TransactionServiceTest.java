package com.btcwallet.service;

import com.btcwallet.config.BitcoinConfig;
import com.btcwallet.exception.BitcoinConfigurationException;
import com.btcwallet.network.BitcoinNodeClient;
import com.btcwallet.network.FeeCalculator;
import com.btcwallet.network.NetworkMonitor;
import com.btcwallet.transaction.Transaction;
import com.btcwallet.transaction.TransactionException;
import com.btcwallet.transaction.TransactionService;
import com.btcwallet.wallet.Wallet;
import com.btcwallet.wallet.WalletService;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.Random;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private WalletService walletService;

    @Mock
    private FeeCalculator feeCalculator;

    @Mock
    private NetworkMonitor networkMonitor;

    private BitcoinNodeClient bitcoinNodeClient;

    private TransactionService transactionService;

    private Wallet testWallet;
    private final String testWalletId = "WALLET-TEST-001";
    private final String testRecipient = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";

    @BeforeEach
    void setUp() throws BitcoinConfigurationException {
        // Create a simple BitcoinConfig for testing that doesn't connect to real nodes
        bitcoinNodeClient = new BitcoinNodeClient(new BitcoinConfig() {
            @Override
            public String getNodeHost() {
                return "localhost";
            }

            @Override
            public int getNodePort() {
                return 8333;
            }

            @Override
            public boolean isTestnet() {
                return false;
            }

            @Override
            public int getTimeoutMillis() {
                return 5000;
            }

            @Override
            public int getMaxConnections() {
                return 1;
            }

            @Override
            public boolean isEnabled() {
                return false;
            } // Disable real node in tests

            @Override
            public boolean isLocalhostPeer() {
                return true;
            }

            @Override
            public NetworkParameters getNetworkParameters() {
                return MainNetParams.get();
            }
        });

        transactionService = new TransactionService(walletService, feeCalculator, networkMonitor, bitcoinNodeClient);

        // Create test wallet
        ECKey ecKey = new ECKey();
        testWallet = Wallet.fromECKey(testWalletId, ecKey, MainNetParams.get());
    }

    @Test
    void testCreateTransactionWithInputsAndChange() throws Exception {
        // Given
        long amount = 100000; // 0.001 BTC
        long fee = 5000; // 0.00005 BTC

        // Mock UTXOs: one large UTXO of 200,000 satoshis
        com.btcwallet.balance.WalletBalance.UTXO utxo = new com.btcwallet.balance.WalletBalance.UTXO(
                "0000000000000000000000000000000000000000000000000000000000000000",
                0,
                org.bitcoinj.core.Coin.valueOf(200000),
                "scriptPubKey",
                10);
        com.btcwallet.balance.WalletBalance balance = new com.btcwallet.balance.WalletBalance(
                testWalletId,
                org.bitcoinj.core.Coin.valueOf(200000),
                org.bitcoinj.core.Coin.ZERO,
                org.bitcoinj.core.Coin.valueOf(200000),
                java.time.Instant.now(),
                "100",
                java.util.List.of(utxo));

        when(walletService.getWallet(testWalletId)).thenReturn(testWallet);
        when(walletService.isValidAddress(testRecipient)).thenReturn(true);
        when(walletService.getWalletBalance(testWalletId)).thenReturn(balance);
        when(feeCalculator.calculateFee(any())).thenReturn(fee);

        // When
        Transaction transaction = transactionService.createTransaction(
                testWalletId, testRecipient, amount, true);

        // Then
        assertNotNull(transaction);
        org.bitcoinj.core.Transaction rawTx = transaction.rawTransaction();

        // Should have 1 input (from our mock UTXO)
        assertEquals(1, rawTx.getInputs().size());

        // Should have 2 outputs: recipient + change
        assertEquals(2, rawTx.getOutputs().size());

        // Recipient output
        assertEquals(amount, rawTx.getOutput(0).getValue().getValue());

        // Change output: 200,000 - 100,000 - 5,000 = 95,000
        assertEquals(95000, rawTx.getOutput(1).getValue().getValue());
    }

    @Test
    void testCreateTransactionSimulation() throws Exception {
        // Given
        long amount = 100000; // 0.001 BTC
        long fee = 5000; // 0.00005 BTC

        // Mock UTXO
        com.btcwallet.balance.WalletBalance.UTXO utxo = new com.btcwallet.balance.WalletBalance.UTXO(
                "0000000000000000000000000000000000000000000000000000000000000000",
                0,
                org.bitcoinj.core.Coin.valueOf(200000),
                "scriptPubKey",
                10);
        com.btcwallet.balance.WalletBalance balance = new com.btcwallet.balance.WalletBalance(
                testWalletId,
                org.bitcoinj.core.Coin.valueOf(200000),
                org.bitcoinj.core.Coin.ZERO,
                org.bitcoinj.core.Coin.valueOf(200000),
                java.time.Instant.now(),
                "100",
                java.util.List.of(utxo));

        // Mock services
        when(walletService.getWallet(testWalletId)).thenReturn(testWallet);
        when(walletService.isValidAddress(testRecipient)).thenReturn(true);
        when(walletService.getWalletBalance(testWalletId)).thenReturn(balance);
        when(feeCalculator.calculateFee(any(org.bitcoinj.core.Transaction.class))).thenReturn(fee);

        // When
        Transaction transaction = transactionService.createTransaction(
                testWalletId, testRecipient, amount, true);

        // Then
        assertNotNull(transaction);
        assertEquals(testWalletId, transaction.walletId());
        assertEquals(testRecipient, transaction.recipientAddress());
        assertEquals(amount, transaction.amount());
        assertEquals(fee, transaction.fee());
        assertEquals(Transaction.TransactionStatus.SIMULATED, transaction.status());
        assertTrue(transaction.isSimulation());
    }

    @Test
    void testCreateTransactionReal() throws Exception {
        // Given
        long amount = 100000; // 0.001 BTC
        long fee = 5000; // 0.00005 BTC

        // Mock UTXO
        com.btcwallet.balance.WalletBalance.UTXO utxo = new com.btcwallet.balance.WalletBalance.UTXO(
                "0000000000000000000000000000000000000000000000000000000000000000",
                0,
                org.bitcoinj.core.Coin.valueOf(200000),
                "scriptPubKey",
                10);
        com.btcwallet.balance.WalletBalance balance = new com.btcwallet.balance.WalletBalance(
                testWalletId,
                org.bitcoinj.core.Coin.valueOf(200000),
                org.bitcoinj.core.Coin.ZERO,
                org.bitcoinj.core.Coin.valueOf(200000),
                java.time.Instant.now(),
                "100",
                java.util.List.of(utxo));

        // Mock services
        when(walletService.getWallet(testWalletId)).thenReturn(testWallet);
        when(walletService.isValidAddress(testRecipient)).thenReturn(true);
        when(walletService.getWalletBalance(testWalletId)).thenReturn(balance);
        when(feeCalculator.calculateFee(any(org.bitcoinj.core.Transaction.class))).thenReturn(fee);
        when(networkMonitor.isNetworkAvailable()).thenReturn(true);
        when(networkMonitor.getMempoolSize()).thenReturn(3000);

        // When
        Transaction transaction = transactionService.createTransaction(
                testWalletId, testRecipient, amount, false);

        // Then
        assertNotNull(transaction);
        assertEquals(testWalletId, transaction.walletId());
        assertEquals(testRecipient, transaction.recipientAddress());
        assertEquals(amount, transaction.amount());
        assertEquals(fee, transaction.fee());
        assertEquals(Transaction.TransactionStatus.BROADCASTED, transaction.status());
        assertFalse(transaction.isSimulation());
    }

    @Test
    void testCreateTransactionInvalidWallet() {
        // Given
        when(walletService.getWallet("invalid-wallet")).thenReturn(null);

        // When/Then
        assertThrows(TransactionException.class, () -> {
            transactionService.createTransaction(
                    "invalid-wallet", testRecipient, 100000, true);
        });
    }

    @Test
    void testCreateTransactionInvalidAddress() {
        // Given
        when(walletService.getWallet(testWalletId)).thenReturn(testWallet);
        when(walletService.isValidAddress("invalid-address")).thenReturn(false);

        // When/Then
        assertThrows(TransactionException.class, () -> {
            transactionService.createTransaction(
                    testWalletId, "invalid-address", 100000, true);
        });
    }

    @Test
    void testCreateTransactionZeroAmount() {
        // When/Then
        // Amount validation happens before wallet lookup, so no stubbing needed
        assertThrows(TransactionException.class, () -> {
            transactionService.createTransaction(
                    testWalletId, testRecipient, 0, true);
        });
    }

    @Test
    void testCreateTransactionNegativeAmount() {
        // When/Then
        // Amount validation happens before wallet lookup, so no stubbing needed
        assertThrows(TransactionException.class, () -> {
            transactionService.createTransaction(
                    testWalletId, testRecipient, -100, true);
        });
    }

    @Test
    void testCreateTransactionNetworkUnavailable() throws Exception {
        // Given
        // Mock UTXO
        com.btcwallet.balance.WalletBalance.UTXO utxo = new com.btcwallet.balance.WalletBalance.UTXO(
                "0000000000000000000000000000000000000000000000000000000000000000",
                0,
                org.bitcoinj.core.Coin.valueOf(200000),
                "scriptPubKey",
                10);
        com.btcwallet.balance.WalletBalance balance = new com.btcwallet.balance.WalletBalance(
                testWalletId,
                org.bitcoinj.core.Coin.valueOf(200000),
                org.bitcoinj.core.Coin.ZERO,
                org.bitcoinj.core.Coin.valueOf(200000),
                java.time.Instant.now(),
                "100",
                java.util.List.of(utxo));

        when(walletService.getWallet(testWalletId)).thenReturn(testWallet);
        when(walletService.isValidAddress(testRecipient)).thenReturn(true);
        when(walletService.getWalletBalance(testWalletId)).thenReturn(balance);
        when(networkMonitor.isNetworkAvailable()).thenReturn(false);

        // Mock the transaction creation to bypass BitcoinJ signing issues
        // We want to test network availability, not transaction signing
        when(feeCalculator.calculateFee(any())).thenReturn(5000L);

        // When/Then
        TransactionException exception = assertThrows(TransactionException.class, () -> {
            transactionService.createTransaction(
                    testWalletId, testRecipient, 100000, false);
        });

        // Verify it's a network-related error
        assertTrue(exception.getMessage().toLowerCase().contains("network"));
    }

    @Test
    void testGetTransactionStatus() {
        // When
        Transaction.TransactionStatus status = transactionService.getTransactionStatus("test-tx-id");

        // Then
        assertEquals(Transaction.TransactionStatus.CONFIRMING, status);
    }

    @Test
    void testListTransactions() {
        // When
        var transactions = transactionService.listTransactions(testWalletId);

        // Then
        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }

    @Test
    void testTransactionValidation() throws Exception {
        // Given
        // Mock UTXO
        com.btcwallet.balance.WalletBalance.UTXO utxo = new com.btcwallet.balance.WalletBalance.UTXO(
                "0000000000000000000000000000000000000000000000000000000000000000",
                0,
                org.bitcoinj.core.Coin.valueOf(200000),
                "scriptPubKey",
                10);
        com.btcwallet.balance.WalletBalance balance = new com.btcwallet.balance.WalletBalance(
                testWalletId,
                org.bitcoinj.core.Coin.valueOf(200000),
                org.bitcoinj.core.Coin.ZERO,
                org.bitcoinj.core.Coin.valueOf(200000),
                java.time.Instant.now(),
                "100",
                java.util.List.of(utxo));

        when(walletService.getWallet(testWalletId)).thenReturn(testWallet);
        when(walletService.isValidAddress(testRecipient)).thenReturn(true);
        when(walletService.getWalletBalance(testWalletId)).thenReturn(balance);
        when(feeCalculator.calculateFee(any())).thenReturn(5000L);

        // Test that validation catches invalid transactions (e.g. amount too high)
        assertThrows(TransactionException.class, () -> {
            transactionService.createTransaction(
                    testWalletId, testRecipient, 500000, true);
        });
    }
}