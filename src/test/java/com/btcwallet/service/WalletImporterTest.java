package com.btcwallet.service;

import com.btcwallet.wallet.Wallet;
import com.btcwallet.wallet.WalletException;
import com.btcwallet.wallet.WalletImporter;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("IMPORTED-"));
        assertEquals(ecKey.getPublicKeyAsHex(), wallet.publicKey());
        assertEquals(privateKeyHex, wallet.privateKey());
        assertEquals(MainNetParams.get(), wallet.networkParameters());
        
        // Verify the address matches
        assertEquals(org.bitcoinj.core.LegacyAddress.fromKey(MainNetParams.get(), ecKey).toString(), wallet.address());
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
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("IMPORTED-"));
        assertNotNull(wallet.address());
        assertNotNull(wallet.publicKey());
        assertNotNull(wallet.privateKey());
        assertEquals(TestNet3Params.get(), wallet.networkParameters());
    }

    @Test
    void testImportFromMnemonicStandardDerivation() {
        // Given
        NetworkParameters params = MainNetParams.get();
        WalletImporter importer = new WalletImporter(params);
        String mnemonic = "army van defense carry jealous true garbage claim echo media make crunch";
        
        // Calculate EXPECTED address using strict BIP44 standard derivation
        // m / 44' / 0' / 0' / 0 / 0
        List<String> words = List.of(mnemonic.split("\\s+"));
        byte[] seed = MnemonicCode.toSeed(words, "");
        DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed);
        
        // Derive: m / 44' / 0' / 0' / 0 / 0
        DeterministicKey purposeKey = HDKeyDerivation.deriveChildKey(masterKey, new ChildNumber(44, true));
        DeterministicKey coinTypeKey = HDKeyDerivation.deriveChildKey(purposeKey, new ChildNumber(0, true));
        DeterministicKey accountKey = HDKeyDerivation.deriveChildKey(coinTypeKey, new ChildNumber(0, true));
        DeterministicKey changeKey = HDKeyDerivation.deriveChildKey(accountKey, new ChildNumber(0, false));
        DeterministicKey addressKey = HDKeyDerivation.deriveChildKey(changeKey, new ChildNumber(0, false));
        
        String expectedAddress = org.bitcoinj.core.LegacyAddress.fromKey(params, addressKey).toString();

        // When
        Wallet wallet = importer.importFromMnemonic(mnemonic);

        // Then
        assertEquals(expectedAddress, wallet.address(), "The imported wallet address does not match the standard BIP44 derivation path (m/44'/0'/0'/0/0)");
    }

    @Test
    void testImportFromWIF() {
        // Given
        WalletImporter importer = new WalletImporter(MainNetParams.get());
        ECKey key = new ECKey();
        String wifPrivateKey = key.getPrivateKeyAsWiF(MainNetParams.get());

        // When
        Wallet wallet = importer.importFromWIF(wifPrivateKey);

        // Then
        assertNotNull(wallet);
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("IMPORTED-"));
        assertEquals(key.getPublicKeyAsHex(), wallet.publicKey());
        assertEquals(key.getPrivateKeyAsHex(), wallet.privateKey());
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
        assertNotEquals(mainNetWallet.address(), testNetWallet.address());
        assertNotEquals(mainNetWallet.networkParameters(), testNetWallet.networkParameters());
        
        // But the public and private keys should be the same
        assertEquals(mainNetWallet.publicKey(), testNetWallet.publicKey());
        assertEquals(mainNetWallet.privateKey(), testNetWallet.privateKey());
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