package com.btcwallet.model;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    private final NetworkParameters mainNet = MainNetParams.get();
    private final NetworkParameters testNet = TestNet3Params.get();

    @Test
    void testWalletCreationFromECKey() {
        // Given
        ECKey ecKey = new ECKey();
        String walletId = "TEST-12345678";

        // When
        Wallet wallet = Wallet.fromECKey(walletId, ecKey, mainNet);

        // Then
        assertNotNull(wallet);
        assertEquals(walletId, wallet.getWalletId());
        assertNotNull(wallet.getAddress());
        assertTrue(wallet.getAddress().startsWith("1") || wallet.getAddress().startsWith("3")); // MainNet addresses
        assertNotNull(wallet.getPublicKey());
        assertNotNull(wallet.getPrivateKey());
        assertNotNull(wallet.getCreatedAt());
        assertTrue(wallet.getCreatedAt().isBefore(Instant.now().plusSeconds(1)));
        assertTrue(wallet.getCreatedAt().isAfter(Instant.now().minusSeconds(1)));
        assertEquals(mainNet, wallet.getNetworkParameters());
    }

    @Test
    void testWalletEquality() {
        // Given
        ECKey ecKey = new ECKey();
        String walletId = "TEST-12345678";
        Wallet wallet1 = Wallet.fromECKey(walletId, ecKey, mainNet);
        Wallet wallet2 = Wallet.fromECKey(walletId, ecKey, mainNet);
        Wallet wallet3 = Wallet.fromECKey("DIFFERENT-ID", ecKey, mainNet);

        // Then
        assertEquals(wallet1, wallet2);
        assertNotEquals(wallet1, wallet3);
        assertEquals(wallet1.hashCode(), wallet2.hashCode());
        assertNotEquals(wallet1.hashCode(), wallet3.hashCode());
    }

    @Test
    void testWalletToECKeyConversion() {
        // Given
        ECKey originalKey = new ECKey();
        String walletId = "TEST-12345678";
        Wallet wallet = Wallet.fromECKey(walletId, originalKey, testNet);

        // When
        ECKey convertedKey = wallet.toECKey();

        // Then
        assertNotNull(convertedKey);
        assertEquals(originalKey.getPublicKeyAsHex(), convertedKey.getPublicKeyAsHex());
        assertEquals(originalKey.getPrivateKeyAsHex(), convertedKey.getPrivateKeyAsHex());
    }

    @Test
    void testWalletToString() {
        // Given
        ECKey ecKey = new ECKey();
        String walletId = "TEST-12345678";
        Wallet wallet = Wallet.fromECKey(walletId, ecKey, mainNet);

        // When
        String walletString = wallet.toString();

        // Then
        assertNotNull(walletString);
        assertTrue(walletString.contains(walletId));
        assertTrue(walletString.contains(wallet.getAddress()));
        assertTrue(walletString.contains("MainNet"));
        assertTrue(walletString.contains("publicKey="));
    }

    @Test
    void testWalletNetworkParameters() {
        // Given
        ECKey ecKey = new ECKey();
        String walletId = "TEST-12345678";

        // When
        Wallet mainNetWallet = Wallet.fromECKey(walletId, ecKey, mainNet);
        Wallet testNetWallet = Wallet.fromECKey(walletId, ecKey, testNet);

        // Then
        assertEquals(mainNet, mainNetWallet.getNetworkParameters());
        assertEquals(testNet, testNetWallet.getNetworkParameters());
        
        // MainNet addresses should start with 1 or 3
        assertTrue(mainNetWallet.getAddress().startsWith("1") || mainNetWallet.getAddress().startsWith("3"));
        
        // TestNet addresses should start with m or n or 2
        String testNetAddress = testNetWallet.getAddress();
        assertTrue(testNetAddress.startsWith("m") || testNetAddress.startsWith("n") || testNetAddress.startsWith("2"));
    }

    @Test
    void testWalletImmutability() {
        // Given
        ECKey ecKey = new ECKey();
        String walletId = "TEST-12345678";
        Wallet wallet = Wallet.fromECKey(walletId, ecKey, mainNet);
        Instant createdAt = wallet.getCreatedAt();

        // When we try to wait a bit
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }

        // Then - createdAt should not change
        assertEquals(createdAt, wallet.getCreatedAt());
        assertEquals(walletId, wallet.getWalletId());
        assertEquals(mainNet, wallet.getNetworkParameters());
    }
}