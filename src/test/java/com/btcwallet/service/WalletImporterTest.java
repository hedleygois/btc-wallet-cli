package com.btcwallet.service;

import com.btcwallet.exception.WalletException;
import com.btcwallet.model.Wallet;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalletImporterTest {

    @Test
    void testImportFromPrivateKey() {
        // Given
        WalletImporter importer = new WalletImporter(MainNetParams.get());
        ECKey ecKey = new ECKey();
        String privateKeyHex = ecKey.getPrivateKeyAsHex();

        // When
        Wallet wallet = importer.importFromPrivateKey(privateKeyHex);

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.getWalletId());
        assertTrue(wallet.getWalletId().startsWith("IMPORTED-"));
        assertEquals(ecKey.getPublicKeyAsHex(), wallet.getPublicKey());
        assertEquals(privateKeyHex, wallet.getPrivateKey());
        assertEquals(MainNetParams.get(), wallet.getNetworkParameters());
        
        // Verify the address matches
        assertEquals(org.bitcoinj.core.LegacyAddress.fromKey(MainNetParams.get(), ecKey).toString(), wallet.getAddress());
    }

    @Test
    void testImportFromMnemonic() {
        // Given
        WalletImporter importer = new WalletImporter(TestNet3Params.get());
        // Use a known valid mnemonic for testing
        String validMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

        // When
        Wallet wallet = importer.importFromMnemonic(validMnemonic);

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.getWalletId());
        assertTrue(wallet.getWalletId().startsWith("IMPORTED-"));
        assertNotNull(wallet.getAddress());
        assertNotNull(wallet.getPublicKey());
        assertNotNull(wallet.getPrivateKey());
        assertEquals(TestNet3Params.get(), wallet.getNetworkParameters());
    }

    @Test
    void testImportFromWIF() {
        // Given
        WalletImporter importer = new WalletImporter(MainNetParams.get());
        String wifPrivateKey = "L3p8oAcQTtuokSCJ36iKm7F2YfXwJra3T9a3H3X7sJ4F5J6J7K8"; // Example WIF

        // When/Then - WIF import is not fully implemented in this version
        WalletException exception = assertThrows(WalletException.class, () -> {
            importer.importFromWIF(wifPrivateKey);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("WIF import not fully implemented"));
    }

    @Test
    void testImportFromInvalidPrivateKey() {
        // Given
        WalletImporter importer = new WalletImporter(MainNetParams.get());
        String invalidPrivateKey = "invalid-hex-key";

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            importer.importFromPrivateKey(invalidPrivateKey);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("private key"));
    }

    @Test
    void testImportFromEmptyMnemonic() {
        // Given
        WalletImporter importer = new WalletImporter(MainNetParams.get());
        String emptyMnemonic = ""; // Empty string should trigger validation error

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            importer.importFromMnemonic(emptyMnemonic);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("Mnemonic seed phrase cannot be null or empty"));
    }

    @Test
    void testImportFromInvalidWIF() {
        // Given
        WalletImporter importer = new WalletImporter(MainNetParams.get());
        String invalidWIF = "invalid-wif-format";

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            importer.importFromWIF(invalidWIF);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("[INPUT ERROR]"));
        assertTrue(exception.getMessage().contains("private key"));
    }

    @Test
    void testImportConsistency() {
        // Given
        WalletImporter mainNetImporter = new WalletImporter(MainNetParams.get());
        WalletImporter testNetImporter = new WalletImporter(TestNet3Params.get());
        
        ECKey ecKey = new ECKey();
        String privateKeyHex = ecKey.getPrivateKeyAsHex();

        // When
        Wallet mainNetWallet = mainNetImporter.importFromPrivateKey(privateKeyHex);
        Wallet testNetWallet = testNetImporter.importFromPrivateKey(privateKeyHex);

        // Then
        assertNotEquals(mainNetWallet.getAddress(), testNetWallet.getAddress());
        assertNotEquals(mainNetWallet.getNetworkParameters(), testNetWallet.getNetworkParameters());
        
        // But the public and private keys should be the same
        assertEquals(mainNetWallet.getPublicKey(), testNetWallet.getPublicKey());
        assertEquals(mainNetWallet.getPrivateKey(), testNetWallet.getPrivateKey());
    }

    @Test
    void testImportFromEmptyPrivateKey() {
        // Given
        WalletImporter importer = new WalletImporter(MainNetParams.get());
        String emptyPrivateKey = "";

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            importer.importFromPrivateKey(emptyPrivateKey);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
    }

    @Test
    void testImportFromNullPrivateKey() {
        // Given
        WalletImporter importer = new WalletImporter(MainNetParams.get());
        String nullPrivateKey = null;

        // When/Then
        WalletException exception = assertThrows(WalletException.class, () -> {
            importer.importFromPrivateKey(nullPrivateKey);
        });
        
        assertEquals(WalletException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }
}