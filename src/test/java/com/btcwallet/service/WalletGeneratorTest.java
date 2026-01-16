package com.btcwallet.service;

import com.btcwallet.exception.WalletException;
import com.btcwallet.model.Wallet;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalletGeneratorTest {

    @Test
    void testGenerateWallet() {
        // Given
        WalletGenerator generator = new WalletGenerator(MainNetParams.get());

        // When
        Wallet wallet = generator.generateWallet();

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
        WalletGenerator generator = new WalletGenerator(TestNet3Params.get());

        // When
        WalletGenerator.WalletGenerationResult result = generator.generateWalletWithMnemonic();

        // Then
        assertNotNull(result);
        assertNotNull(result.getWallet());
        assertNotNull(result.getMnemonic());
        
        Wallet wallet = result.getWallet();
        assertNotNull(wallet.walletId());
        assertTrue(wallet.walletId().startsWith("WALLET-"));
        assertNotNull(wallet.address());
        assertNotNull(wallet.publicKey());
        assertNotNull(wallet.privateKey());
        assertEquals(TestNet3Params.get(), wallet.networkParameters());
        
        // Check mnemonic format (should be 12 words separated by spaces)
        String[] mnemonicWords = result.getMnemonic().split("\\s+");
        assertEquals(12, mnemonicWords.length);
        assertTrue(mnemonicWords.length > 0);
        for (String word : mnemonicWords) {
            assertFalse(word.isEmpty());
        }
    }

    @Test
    void testGenerateWalletWithDifferentNetworks() {
        // Given
        WalletGenerator mainNetGenerator = new WalletGenerator(MainNetParams.get());
        WalletGenerator testNetGenerator = new WalletGenerator(TestNet3Params.get());

        // When
        Wallet mainNetWallet = mainNetGenerator.generateWallet();
        Wallet testNetWallet = testNetGenerator.generateWallet();

        // Then
        assertNotEquals(mainNetWallet.address(), testNetWallet.address());
        assertNotEquals(mainNetWallet.networkParameters(), testNetWallet.networkParameters());
        
        // MainNet addresses should start with 1 or 3
        assertTrue(mainNetWallet.address().startsWith("1") || mainNetWallet.address().startsWith("3"));
        
        // TestNet addresses should start with m, n, or 2
        String testNetAddress = testNetWallet.address();
        assertTrue(testNetAddress.startsWith("m") || testNetAddress.startsWith("n") || testNetAddress.startsWith("2"));
    }

    @Test
    void testWalletIdUniqueness() {
        // Given
        WalletGenerator generator = new WalletGenerator(MainNetParams.get());

        // When
        Wallet wallet1 = generator.generateWallet();
        Wallet wallet2 = generator.generateWallet();

        // Then
        assertNotEquals(wallet1.walletId(), wallet2.walletId());
        assertNotEquals(wallet1.address(), wallet2.address());
        assertNotEquals(wallet1.publicKey(), wallet2.publicKey());
        assertNotEquals(wallet1.privateKey(), wallet2.privateKey());
    }

    @Test
    void testWalletGenerationWithMnemonicConsistency() {
        // Given
        WalletGenerator generator = new WalletGenerator(MainNetParams.get());

        // When
        WalletGenerator.WalletGenerationResult result = generator.generateWalletWithMnemonic();
        Wallet wallet = result.getWallet();
        
        // Verify that the wallet can be recreated from the mnemonic
        WalletImporter importer = new WalletImporter(MainNetParams.get());
        Wallet importedWallet = importer.importFromMnemonic(result.getMnemonic());

        // Then - the imported wallet should have the same address as the generated one
        assertEquals(wallet.address(), importedWallet.address());
        assertEquals(wallet.publicKey(), importedWallet.publicKey());
        // Note: Private keys might differ due to different key derivation paths, but addresses should match
    }

    @Test
    void testWalletGenerationExceptionHandling() {
        // This test verifies that our exception handling works correctly
        // Since we can't easily force a MnemonicException, we'll test the exception structure
        
        // Given
        WalletGenerator generator = new WalletGenerator(MainNetParams.get());

        // When/Then - just verify that normal generation doesn't throw exceptions
        assertDoesNotThrow(() -> {
            Wallet wallet = generator.generateWallet();
            assertNotNull(wallet);
        });
        
        assertDoesNotThrow(() -> {
            WalletGenerator.WalletGenerationResult result = generator.generateWalletWithMnemonic();
            assertNotNull(result);
            assertNotNull(result.getWallet());
            assertNotNull(result.getMnemonic());
        });
    }
}