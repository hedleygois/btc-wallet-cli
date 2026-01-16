package com.btcwallet.model;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a Bitcoin wallet containing a key pair and address.
 * This is an immutable record class (Java 16+ feature).
 *
 * @param walletId Unique identifier for the wallet
 * @param address Bitcoin address
 * @param publicKey Public key in hex format
 * @param privateKey Private key in hex format (sensitive!)
 * @param createdAt Timestamp when wallet was created
 * @param networkParameters Network parameters (MainNet, TestNet, etc.)
 */
public record Wallet(String walletId, String address, String publicKey, String privateKey, 
                     Instant createdAt, NetworkParameters networkParameters) implements Serializable {
    
    /**
     * Compact constructor for validation and default values.
     */
    public Wallet {
        if (walletId == null || walletId.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet ID cannot be null or empty");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be null or empty");
        }
        if (publicKey == null || publicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be null or empty");
        }
        if (privateKey == null || privateKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Private key cannot be null or empty");
        }
        if (networkParameters == null) {
            throw new IllegalArgumentException("Network parameters cannot be null");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    /**
     * Creates a Wallet from an ECKey (BitcoinJ key pair).
     *
     * @param walletId Unique identifier for the wallet
     * @param ecKey BitcoinJ ECKey containing the key pair
     * @param networkParameters Network parameters
     * @return New Wallet instance
     */
    public static Wallet fromECKey(String walletId, ECKey ecKey, NetworkParameters networkParameters) {
        var address = org.bitcoinj.core.LegacyAddress.fromKey(networkParameters, ecKey);
        return new Wallet(
            walletId,
            address.toString(),
            ecKey.getPublicKeyAsHex(),
            ecKey.getPrivateKeyAsHex(),
            Instant.now(),
            networkParameters
        );
    }
    
    /**
     * Gets the ECKey representation of this wallet.
     *
     * @return ECKey containing the wallet's key pair
     */
    public ECKey toECKey() {
        return ECKey.fromPrivate(org.bitcoinj.core.Utils.HEX.decode(privateKey));
    }
    
    @Override
    public String toString() {
        return "Wallet[" +
                "walletId='" + walletId + '\'' +
                ", address='" + address + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", createdAt=" + createdAt +
                ", network=" + (networkParameters.getId().equals(MainNetParams.get().getId()) ? "MainNet" : "TestNet") +
                ']';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallet wallet = (Wallet) o;
        return walletId.equals(wallet.walletId);
    }
    
    @Override
    public int hashCode() {
        return walletId.hashCode();
    }
}