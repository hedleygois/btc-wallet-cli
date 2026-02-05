package com.btcwallet.service;

import com.btcwallet.balance.BalanceCache;
import com.btcwallet.balance.WalletBalance;
import com.btcwallet.balance.BalanceException;
import com.btcwallet.network.BitcoinNodeClient;
import com.btcwallet.wallet.Wallet;
import com.btcwallet.wallet.WalletException;
import com.btcwallet.wallet.WalletGenerator;
import com.btcwallet.wallet.WalletService;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Coin;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    @Mock
    private BitcoinNodeClient bitcoinNodeClient;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        BalanceCache.getInstance().clearAll();
        this.walletService = new WalletService(MainNetParams.get(), bitcoinNodeClient);
    }

    @AfterEach
    void tearDown() {
        if (walletService != null) {
            walletService.shutdown();
        }
    }

    @Test
    void testWalletServiceDefaultConstructor() {
        // Given/When
        WalletService service = new WalletService(MainNetParams.get(), null);

        // Then
        assertNotNull(service);
        assertEquals(MainNetParams.get(), service.getNetworkParameters());
        assertEquals("MainNet", service.getNetworkName());
        service.shutdown();
    }

    @Test
    void testGenerateWallet() {
        // When
        Wallet wallet = walletService.generateWallet();

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("WALLET-"));
        assertNotNull(wallet.address());
        assertNotNull(wallet.publicKey());
        assertNotNull(wallet.privateKey());
        assertEquals(MainNetParams.get(), wallet.networkParameters());
        assertNotNull(walletService.getWallet(wallet.walletId()));
    }

    @Test
    void testGenerateWalletWithMnemonic() {
        // Setup for TestNet
        walletService = new WalletService(TestNet3Params.get(), bitcoinNodeClient);

        // When
        WalletGenerator.WalletGenerationResult result = walletService.generateWalletWithMnemonic();

        // Then
        assertNotNull(result);
        assertNotNull(result.getWallet());
        assertNotNull(result.getMnemonic());
        
        Wallet wallet = result.getWallet();
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("WALLET-"));
        assertNotNull(wallet.address());
        assertEquals(TestNet3Params.get(), wallet.networkParameters());
        
        // Verify mnemonic format
        String[] words = result.getMnemonic().split("\\s+");
        assertEquals(12, words.length);
        assertNotNull(walletService.getWallet(wallet.walletId()));
    }

    @Test
    void testImportFromPrivateKey() {
        // Given
        ECKey ecKey = new ECKey();
        String privateKeyHex = ecKey.getPrivateKeyAsHex();

        // When
        Wallet wallet = walletService.importFromPrivateKey(privateKeyHex);

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("IMPORTED-"));
        assertEquals(ecKey.getPublicKeyAsHex(), wallet.publicKey());
        assertEquals(privateKeyHex, wallet.privateKey());
        assertEquals(MainNetParams.get(), wallet.networkParameters());
        assertNotNull(walletService.getWallet(wallet.walletId()));
    }

    @Test
    void testImportFromMnemonic() {
        // Given
        String validMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

        // When
        Wallet wallet = walletService.importFromMnemonic(validMnemonic);

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("IMPORTED-"));
        assertNotNull(wallet.address());
        assertEquals(MainNetParams.get(), wallet.networkParameters());
        assertNotNull(walletService.getWallet(wallet.walletId()));
    }

    @Test
    void testImportFromWIF() {
        // Given
        ECKey key = new ECKey();
        String wifPrivateKey = key.getPrivateKeyAsWiF(MainNetParams.get());

        // When
        Wallet wallet = walletService.importFromWIF(wifPrivateKey);

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("IMPORTED-"));
        assertEquals(key.getPublicKeyAsHex(), wallet.publicKey());
        assertEquals(key.getPrivateKeyAsHex(), wallet.privateKey());
        assertNotNull(walletService.getWallet(wallet.walletId()));
    }

    @Test
    void testIsValidAddress() {
        // Given
        ECKey ecKey = new ECKey();
        String validAddress = org.bitcoinj.core.LegacyAddress.fromKey(MainNetParams.get(), ecKey).toString();
        String invalidAddress = "invalid-bitcoin-address";

        // When/Then
        assertTrue(walletService.isValidAddress(validAddress));
        assertFalse(walletService.isValidAddress(invalidAddress));
        assertFalse(walletService.isValidAddress(null));
        assertFalse(walletService.isValidAddress(""));
    }

    @Test
    void testInvalidPrivateKeyImport() {
        // Given
        String invalidPrivateKey = "invalid-hex-key";

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            walletService.importFromPrivateKey(invalidPrivateKey);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("private key"));
    }

    @Test
    void testInvalidMnemonicImport() {
        // Given
        String invalidMnemonic = "not a valid mnemonic phrase"; 

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            walletService.importFromMnemonic(invalidMnemonic);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("mnemonic"));
    }

    @Test
    void testInvalidWIFImport() {
        // Given
        String invalidWIF = "invalid-wif-format";

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            walletService.importFromWIF(invalidWIF);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("private key"));
    }

    @Test
    void testCrossNetworkConsistency() {
        // Given
        WalletService mainNetService = new WalletService(MainNetParams.get(), null);
        WalletService testNetService = new WalletService(TestNet3Params.get(), null);
        
        try {
            ECKey ecKey = new ECKey();
            String privateKeyHex = ecKey.getPrivateKeyAsHex();

            // When
            Wallet mainNetWallet = mainNetService.importFromPrivateKey(privateKeyHex);
            Wallet testNetWallet = testNetService.importFromPrivateKey(privateKeyHex);

            // Then
            assertNotEquals(mainNetWallet.address(), testNetWallet.address());
            assertNotEquals(mainNetWallet.networkParameters(), testNetWallet.networkParameters());
            
            // But keys should be the same
            assertEquals(mainNetWallet.publicKey(), testNetWallet.publicKey());
            assertEquals(mainNetWallet.privateKey(), testNetWallet.privateKey());
        } finally {
            mainNetService.shutdown();
            testNetService.shutdown();
        }
    }

    @Test
    void testWalletGenerationAndImportConsistency() {
        // When - generate a wallet with mnemonic
        WalletGenerator.WalletGenerationResult generationResult = walletService.generateWalletWithMnemonic();
        String mnemonic = generationResult.getMnemonic();
        
        // Then - import it back
        Wallet importedWallet = walletService.importFromMnemonic(mnemonic);
        
        // They should have the same address
        assertEquals(generationResult.getWallet().address(), importedWallet.address());
        assertEquals(generationResult.getWallet().publicKey(), importedWallet.publicKey());
    }

    // --- New Tests for isValid* methods ---

    @Test
    void testIsValidHexPrivateKey() {
        // Valid cases
        assertTrue(walletService.isValidHexPrivateKey("18e14a7b6a307f426a94f8114701e7c8e774e7f9a47e2c2035db29a206321725"));
        assertTrue(walletService.isValidHexPrivateKey("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141")); // Max valid key

        // Invalid cases
        assertFalse(walletService.isValidHexPrivateKey("invalid-hex-key")); // Not hex
        assertFalse(walletService.isValidHexPrivateKey("123")); // Too short
        assertFalse(walletService.isValidHexPrivateKey("18e14a7b6a307f426a94f8114701e7c8e774e7f9a47e2c2035db29a206321725a")); // Too long
        assertFalse(walletService.isValidHexPrivateKey("")); // Empty
        assertFalse(walletService.isValidHexPrivateKey(null)); // Null
    }

    @Test
    void testIsValidWIF() {
        // Valid cases (MainNet and TestNet examples)
        assertTrue(walletService.isValidWIF("5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF")); // 51 chars MainNet
        assertTrue(walletService.isValidWIF("L5eX4o4H9eN1eN1eN1eN1eN1eN1eN1eN1eN1eN1eN1eN1eN1eNPQ")); // 52 chars MainNet (starts with L)
        assertTrue(walletService.isValidWIF("cT7oG7qC2eM8T6U9K4L5P3Q8R7S6T5U4V3W2X1Y0Z9A8B7C6D5EF")); // 52 chars TestNet (starts with c)

        // Invalid cases
        assertFalse(walletService.isValidWIF("invalid-wif")); // Not WIF format
        assertFalse(walletService.isValidWIF("5HueCGzWSQh8bVUJPz1j2j6Bf6bVp2b8j9p9Bf6bVp2b8j9p9")); // Too short
        assertFalse(walletService.isValidWIF("5HueCGzWSQh8bVUJPz1j2j6Bf6bVp2b8j9p9Bf6bVp2b8j9p9ABCDE")); // Too long
        assertFalse(walletService.isValidWIF("")); // Empty
        assertFalse(walletService.isValidWIF(null)); // Null
    }

    @Test
    void testIsValidMnemonic() {
        // Valid cases (12, 18, 24 words)
        assertTrue(walletService.isValidMnemonic("abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about")); // 12 words
        assertTrue(walletService.isValidMnemonic("word word word word word word word word word word word word word word word word word word")); // 18 words
        assertTrue(walletService.isValidMnemonic("word word word word word word word word word word word word word word word word word word word word word word word word")); // 24 words

        // Invalid cases
        assertFalse(walletService.isValidMnemonic("abandon abandon abandon")); // Too few words
        assertFalse(walletService.isValidMnemonic("abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about extra")); // Too many words
        assertFalse(walletService.isValidMnemonic("notamnemonicphrase")); // Single word
        assertFalse(walletService.isValidMnemonic("")); // Empty
        assertFalse(walletService.isValidMnemonic(null)); // Null
    }

    // --- New Tests for wallet storage/retrieval methods ---

    @Test
    void testGetWalletById() {
        // Given
        Wallet generatedWallet = walletService.generateWallet();
        String walletId = generatedWallet.walletId();

        // When
        Wallet retrievedWallet = walletService.getWallet(walletId);

        // Then
        assertNotNull(retrievedWallet);
        assertEquals(generatedWallet, retrievedWallet);
        assertNull(walletService.getWallet("NON_EXISTENT_ID"));
    }

    @Test
    void testGetAllWallets() {
        // Given
        walletService.clearWallets(); // Start fresh
        Wallet wallet1 = walletService.generateWallet();
        Wallet wallet2 = walletService.generateWallet();

        // When
        Map<String, Wallet> allWallets = walletService.getAllWallets();

        // Then
        assertEquals(2, allWallets.size());
        assertTrue(allWallets.containsKey(wallet1.walletId()));
        assertTrue(allWallets.containsKey(wallet2.walletId()));
        assertEquals(wallet1, allWallets.get(wallet1.walletId()));
        assertEquals(wallet2, allWallets.get(wallet2.walletId()));
    }

    @Test
    void testClearWallets() {
        // Given
        walletService.generateWallet();
        assertTrue(walletService.getAllWallets().size() > 0);

        // When
        walletService.clearWallets();

        // Then
        assertTrue(walletService.getAllWallets().isEmpty());
    }

    @Test
    void testListWalletIds() {
        // Given
        walletService.clearWallets();
        Wallet wallet1 = walletService.generateWallet();
        Wallet wallet2 = walletService.generateWallet();

        // When
        var walletIds = walletService.listWalletIds();

        // Then
        assertEquals(2, walletIds.size());
        assertTrue(walletIds.contains(wallet1.walletId()));
        assertTrue(walletIds.contains(wallet2.walletId()));
    }

    // --- New Tests for balance-related methods with mocked BalanceService ---

    @Test
    void testGetWalletBalance() throws Exception {
        // Given
        Wallet wallet = walletService.generateWallet();
        String walletId = wallet.walletId();
        Coin confirmed = Coin.valueOf(100000);
        Coin unconfirmed = Coin.valueOf(50000);
        Coin total = confirmed.add(unconfirmed);
        WalletBalance mockBalance = new WalletBalance(walletId, confirmed, unconfirmed, total, Instant.now(), "123456", Collections.emptyList());
        when(bitcoinNodeClient.getWalletBalance(wallet)).thenReturn(mockBalance);

        // When
        WalletBalance retrievedBalance = walletService.getWalletBalance(walletId);

        // Then
        assertNotNull(retrievedBalance);
        assertEquals(mockBalance, retrievedBalance);
        verify(bitcoinNodeClient, times(1)).getWalletBalance(wallet);
    }

    @Test
    void testGetWalletBalance_NoBitcoinNodeClient() throws WalletException {
        // Given
        WalletService serviceWithoutClient = new WalletService(MainNetParams.get(), null);
        Wallet wallet = serviceWithoutClient.generateWallet();
        String walletId = wallet.walletId();

        // When
        WalletBalance balance = serviceWithoutClient.getWalletBalance(walletId);

        // Then
        assertNotNull(balance);
        assertEquals(Coin.ZERO, balance.getTotalBalance());
        serviceWithoutClient.shutdown();
    }

    @Test
    void testGetWalletBalance_BalanceException() throws Exception {
        // Given
        Wallet wallet = walletService.generateWallet();
        String walletId = wallet.walletId();
        when(bitcoinNodeClient.getWalletBalance(wallet)).thenThrow(new RuntimeException("Node error"));

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            walletService.getWalletBalance(walletId);
        });
        assertTrue(exception.getMessage().contains("Failed to refresh balance: Node error"));
        verify(bitcoinNodeClient, times(1)).getWalletBalance(wallet);
    }

    @Test
    void testRefreshWalletBalance() throws Exception {
        // Given
        Wallet wallet = walletService.generateWallet();
        String walletId = wallet.walletId();
        Coin confirmed = Coin.valueOf(200000);
        Coin unconfirmed = Coin.valueOf(100000);
        Coin total = confirmed.add(unconfirmed);
        WalletBalance mockBalance = new WalletBalance(walletId, confirmed, unconfirmed, total, Instant.now(), "123457", Collections.emptyList());
        when(bitcoinNodeClient.getWalletBalance(wallet)).thenReturn(mockBalance);

        // When
        WalletBalance refreshedBalance = walletService.refreshWalletBalance(walletId);

        // Then
        assertNotNull(refreshedBalance);
        assertEquals(mockBalance, refreshedBalance);
        verify(bitcoinNodeClient, times(1)).getWalletBalance(wallet);
    }

    @Test
    void testHasSufficientFunds() throws Exception {
        // Given
        Wallet wallet = walletService.generateWallet();
        String walletId = wallet.walletId();
        Coin amount = Coin.valueOf(50000);
        WalletBalance mockBalance = new WalletBalance(walletId, Coin.valueOf(100000), Coin.ZERO, Coin.valueOf(100000), Instant.now(), "1", Collections.emptyList());
        when(bitcoinNodeClient.getWalletBalance(wallet)).thenReturn(mockBalance);

        // When
        boolean hasFunds = walletService.hasSufficientFunds(walletId, amount);

        // Then
        assertTrue(hasFunds);
    }

    @Test
    void testGetTotalBalance() throws Exception {
        // Given
        walletService.clearWallets();
        Wallet w1 = walletService.generateWallet();
        Wallet w2 = walletService.generateWallet();
        
        when(bitcoinNodeClient.getWalletBalance(w1)).thenReturn(new WalletBalance(w1.walletId(), Coin.valueOf(100), Coin.ZERO, Coin.valueOf(100), Instant.now(), "1", List.of()));
        when(bitcoinNodeClient.getWalletBalance(w2)).thenReturn(new WalletBalance(w2.walletId(), Coin.valueOf(200), Coin.ZERO, Coin.valueOf(200), Instant.now(), "1", List.of()));

        // When
        Coin retrievedTotal = walletService.getTotalBalance();

        // Then
        assertEquals(Coin.valueOf(300), retrievedTotal);
    }
}