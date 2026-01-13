package com.btcwallet.model;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a Bitcoin wallet containing a key pair and address.
 * This class is immutable once created.
 */
public class Wallet implements Serializable {
    private final String walletId;
    private final String address;
    private final String publicKey;
    private final String privateKey;
    private final Instant createdAt;
    private final NetworkParameters networkParameters;
    
    /**
     * Creates a new Wallet instance.
     *
     * @param walletId Unique identifier for the wallet
     * @param address Bitcoin address
     * @param publicKey Public key in hex format
     * @param privateKey Private key in hex format (sensitive!)
     * @param networkParameters Network parameters (MainNet, TestNet, etc.)
     */
    public Wallet(String walletId, String address, String publicKey, String privateKey, NetworkParameters networkParameters) {
        this.walletId = walletId;
        this.address = address;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.createdAt = Instant.now();
        this.networkParameters = networkParameters;
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
        org.bitcoinj.core.Address address = org.bitcoinj.core.LegacyAddress.fromKey(networkParameters, ecKey);
        return new Wallet(
            walletId,
            address.toString(),
            ecKey.getPublicKeyAsHex(),
            ecKey.getPrivateKeyAsHex(),
            networkParameters
        );
    }
    
    // Getters
    public String getWalletId() {
        return walletId;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public String getPrivateKey() {
        return privateKey;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public NetworkParameters getNetworkParameters() {
        return networkParameters;
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
        return "Wallet{" +
                "walletId='" + walletId + '\'' +
                ", address='" + address + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", createdAt=" + createdAt +
                ", network=" + (networkParameters.getId().equals(MainNetParams.get().getId()) ? "MainNet" : "TestNet") +
                '}';
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