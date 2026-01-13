package com.btcwallet.service;

import com.btcwallet.model.Wallet;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;

/**
 * Main service for wallet operations, combining generation and import functionality.
 */
public class WalletService {
    
    private final WalletGenerator walletGenerator;
    private final WalletImporter walletImporter;
    private final NetworkParameters networkParameters;
    
    /**
     * Creates a new WalletService instance.
     * Defaults to MainNet if no network parameters are specified.
     */
    public WalletService() {
        this(new MainNetParams());
    }
    
    /**
     * Creates a new WalletService instance with specific network parameters.
     *
     * @param networkParameters Network parameters to use
     */
    public WalletService(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
        this.walletGenerator = new WalletGenerator(networkParameters);
        this.walletImporter = new WalletImporter(networkParameters);
    }
    
    /**
     * Generates a new Bitcoin wallet.
     *
     * @return Newly generated wallet
     */
    public Wallet generateWallet() {
        return walletGenerator.generateWallet();
    }
    
    /**
     * Generates a new Bitcoin wallet with a mnemonic seed phrase.
     *
     * @return WalletGenerationResult containing wallet and mnemonic
     */
    public WalletGenerator.WalletGenerationResult generateWalletWithMnemonic() {
        return walletGenerator.generateWalletWithMnemonic();
    }
    
    /**
     * Imports a wallet from a private key in hex format.
     *
     * @param privateKeyHex Private key in hexadecimal format
     * @return Imported wallet
     * @throws WalletImporter.WalletImportException If import fails
     */
    public Wallet importFromPrivateKey(String privateKeyHex) {
        return walletImporter.importFromPrivateKey(privateKeyHex);
    }
    
    /**
     * Imports a wallet from a mnemonic seed phrase.
     *
     * @param mnemonic Mnemonic seed phrase
     * @return Imported wallet
     * @throws WalletImporter.WalletImportException If import fails
     */
    public Wallet importFromMnemonic(String mnemonic) {
        return walletImporter.importFromMnemonic(mnemonic);
    }
    
    /**
     * Imports a wallet from a WIF (Wallet Import Format) private key.
     *
     * @param wifPrivateKey WIF formatted private key
     * @return Imported wallet
     * @throws WalletImporter.WalletImportException If import fails
     */
    public Wallet importFromWIF(String wifPrivateKey) {
        return walletImporter.importFromWIF(wifPrivateKey);
    }
    
    /**
     * Validates that a Bitcoin address is valid for the current network.
     *
     * @param address Bitcoin address to validate
     * @return true if address is valid, false otherwise
     */
    public boolean isValidAddress(String address) {
        try {
            org.bitcoinj.core.Address.fromString(networkParameters, address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the current network parameters.
     *
     * @return Current network parameters
     */
    public NetworkParameters getNetworkParameters() {
        return networkParameters;
    }
    
    /**
     * Gets the network name.
     *
     * @return Network name (MainNet, TestNet, etc.)
     */
    public String getNetworkName() {
        if (networkParameters.getId().equals(MainNetParams.get().getId())) {
            return "MainNet";
        } else {
            return "TestNet";
        }
    }
}