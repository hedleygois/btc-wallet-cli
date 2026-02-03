package com.btcwallet.service;

import com.btcwallet.wallet.Wallet;
import com.btcwallet.wallet.WalletException;
import com.btcwallet.wallet.WalletGenerator;
import com.btcwallet.wallet.WalletService;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalletServiceTest {

    @Test
    void testWalletServiceDefaultConstructor() {
        // Given/When
        WalletService service = new WalletService();

        // Then
        assertNotNull(service);
        assertEquals(MainNetParams.get(), service.getNetworkParameters());
        assertEquals("MainNet", service.getNetworkName());
    }

    @Test
    void testWalletServiceWithNetworkParameters() {
        // Given/When
        WalletService mainNetService = new WalletService(MainNetParams.get());
        WalletService testNetService = new WalletService(TestNet3Params.get());

        // Then
        assertEquals(MainNetParams.get(), mainNetService.getNetworkParameters());
        assertEquals("MainNet", mainNetService.getNetworkName());
        
        assertEquals(TestNet3Params.get(), testNetService.getNetworkParameters());
        assertEquals("TestNet", testNetService.getNetworkName());
    }

    @Test
    void testGenerateWallet() {
        // Given
        WalletService service = new WalletService();

        // When
        Wallet wallet = service.generateWallet();

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("WALLET-"));
        assertNotNull(wallet.address());
        assertNotNull(wallet.publicKey());
        assertNotNull(wallet.privateKey());
        assertEquals(MainNetParams.get(), wallet.networkParameters());
    }

    @Test
    void testGenerateWalletWithMnemonic() {
        // Given
        WalletService service = new WalletService(TestNet3Params.get());

        // When
        WalletGenerator.WalletGenerationResult result = service.generateWalletWithMnemonic();

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
    }

    @Test
    void testImportFromPrivateKey() {
        // Given
        WalletService service = new WalletService();
        ECKey ecKey = new ECKey();
        String privateKeyHex = ecKey.getPrivateKeyAsHex();

        // When
        Wallet wallet = service.importFromPrivateKey(privateKeyHex);

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("IMPORTED-"));
        assertEquals(ecKey.getPublicKeyAsHex(), wallet.publicKey());
        assertEquals(privateKeyHex, wallet.privateKey());
        assertEquals(MainNetParams.get(), wallet.networkParameters());
    }

    @Test
    void testImportFromMnemonic() {
        // Given
        WalletService service = new WalletService();
        String validMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

        // When
        Wallet wallet = service.importFromMnemonic(validMnemonic);

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("IMPORTED-"));
        assertNotNull(wallet.address());
        assertEquals(MainNetParams.get(), wallet.networkParameters());
    }

    @Test
    void testImportFromWIF() {
        // Given
        WalletService service = new WalletService();
        ECKey key = new ECKey();
        String wifPrivateKey = key.getPrivateKeyAsWiF(MainNetParams.get());

        // When
        Wallet wallet = service.importFromWIF(wifPrivateKey);

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("IMPORTED-"));
        assertEquals(key.getPublicKeyAsHex(), wallet.publicKey());
        assertEquals(key.getPrivateKeyAsHex(), wallet.privateKey());
    }

    @Test
    void testIsValidAddress() {
        // Given
        WalletService service = new WalletService();
        ECKey ecKey = new ECKey();
        String validAddress = org.bitcoinj.core.LegacyAddress.fromKey(MainNetParams.get(), ecKey).toString();
        String invalidAddress = "invalid-bitcoin-address";

        // When/Then
        assertTrue(service.isValidAddress(validAddress));
        assertFalse(service.isValidAddress(invalidAddress));
        assertFalse(service.isValidAddress(null));
        assertFalse(service.isValidAddress(""));
    }

    @Test
    void testInvalidPrivateKeyImport() {
        // Given
        WalletService service = new WalletService();
        String invalidPrivateKey = "invalid-hex-key";

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            service.importFromPrivateKey(invalidPrivateKey);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("private key"));
    }

    @Test
    void testInvalidMnemonicImport() {
        // Given
        WalletService service = new WalletService();
        String invalidMnemonic = ""; // Empty string will trigger validation error

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            service.importFromMnemonic(invalidMnemonic);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("mnemonic"));
    }

    @Test
    void testInvalidWIFImport() {
        // Given
        WalletService service = new WalletService();
        String invalidWIF = "invalid-wif-format";

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            service.importFromWIF(invalidWIF);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("private key"));
    }

    @Test
    void testCrossNetworkConsistency() {
        // Given
        WalletService mainNetService = new WalletService(MainNetParams.get());
        WalletService testNetService = new WalletService(TestNet3Params.get());
        
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
    }

    @Test
    void testWalletGenerationAndImportConsistency() {
        // Given
        WalletService service = new WalletService();

        // When - generate a wallet with mnemonic
        WalletGenerator.WalletGenerationResult generationResult = service.generateWalletWithMnemonic();
        String mnemonic = generationResult.getMnemonic();
        
        // Then - import it back
        Wallet importedWallet = service.importFromMnemonic(mnemonic);
        
        // They should have the same address
        assertEquals(generationResult.getWallet().address(), importedWallet.address());
        assertEquals(generationResult.getWallet().publicKey(), importedWallet.publicKey());
    }
}