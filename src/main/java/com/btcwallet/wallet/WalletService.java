package com.btcwallet.wallet;

import com.btcwallet.balance.BalanceException;
import com.btcwallet.balance.BalanceService;
import com.btcwallet.balance.WalletBalance;
import com.btcwallet.wallet.WalletGenerator.WalletGenerationResult;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Main service for wallet operations, combining generation and import functionality.
 */
public class WalletService {
    
    private final WalletGenerator walletGenerator;
    private final WalletImporter walletImporter;
    private final NetworkParameters networkParameters;
    private final Map<String, Wallet> walletStorage = new HashMap<>();
    private BalanceService balanceService;
    
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
     * Creates a new WalletService instance with BalanceService integration.
     *
     * @param networkParameters Network parameters to use
     * @param balanceService Balance service for balance operations
     */
    public WalletService(NetworkParameters networkParameters, BalanceService balanceService) {
        this.networkParameters = networkParameters;
        this.walletGenerator = new WalletGenerator(networkParameters);
        this.walletImporter = new WalletImporter(networkParameters);
        this.balanceService = balanceService;
    }
    
    /**
     * Sets the BalanceService for this WalletService.
     *
     * @param balanceService Balance service to use
     */
    public void setBalanceService(BalanceService balanceService) {
        this.balanceService = balanceService;
    }
    
    /**
     * Generates a new Bitcoin wallet.
     *
     * @return Newly generated wallet
     */
    public Wallet generateWallet() {
        Wallet wallet = walletGenerator.generateWallet();
        walletStorage.put(wallet.walletId(), wallet);
        return wallet;
    }
    
    /**
     * Generates a new Bitcoin wallet with a mnemonic seed phrase.
     *
     * @return WalletGenerationResult containing wallet and mnemonic
     */
    public WalletGenerator.WalletGenerationResult generateWalletWithMnemonic() {
        WalletGenerator.WalletGenerationResult result = walletGenerator.generateWalletWithMnemonic();
        walletStorage.put(result.getWallet().walletId(), result.getWallet());
        return result;
    }
    
    /**
     * Imports a wallet from a private key in hex format.
     *
     * @param privateKeyHex Private key in hexadecimal format
     * @return Imported wallet
     * @throws WalletImporter.WalletImportException If import fails
     */
    public Wallet importFromPrivateKey(String privateKeyHex) {
        Wallet wallet = walletImporter.importFromPrivateKey(privateKeyHex);
        walletStorage.put(wallet.walletId(), wallet);
        return wallet;
    }
    
    /**
     * Imports a wallet from a mnemonic seed phrase.
     *
     * @param mnemonic Mnemonic seed phrase
     * @return Imported wallet
     * @throws WalletImporter.WalletImportException If import fails
     */
    public Wallet importFromMnemonic(String mnemonic) {
        Wallet wallet = walletImporter.importFromMnemonic(mnemonic);
        walletStorage.put(wallet.walletId(), wallet);
        return wallet;
    }
    
    /**
     * Imports a wallet from a WIF (Wallet Import Format) private key.
     *
     * @param wifPrivateKey WIF formatted private key
     * @return Imported wallet
     * @throws WalletImporter.WalletImportException If import fails
     */
    public Wallet importFromWIF(String wifPrivateKey) {
        Wallet wallet = walletImporter.importFromWIF(wifPrivateKey);
        walletStorage.put(wallet.walletId(), wallet);
        return wallet;
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

    /**
     * Gets a wallet by its ID.
     *
     * @param walletId Wallet ID to retrieve
     * @return Wallet if found, null otherwise
     */
    public Wallet getWallet(String walletId) {
        return walletStorage.get(walletId);
    }

    /**
     * Gets all stored wallets.
     *
     * @return Map of wallet IDs to wallets
     */
    public Map<String, Wallet> getAllWallets() {
        return new HashMap<>(walletStorage);
    }

    /**
     * Clears all stored wallets.
     * Useful for testing.
     */
    public void clearWallets() {
        walletStorage.clear();
    }

    /**
     * Gets the balance for a wallet.
     * 
     * @param walletId Wallet ID
     * @return WalletBalance object
     * @throws WalletException If balance cannot be retrieved
     */
    public WalletBalance getWalletBalance(String walletId) throws WalletException {
        if (balanceService == null) {
            throw WalletException.balanceError("Balance service not available");
        }
        
        try {
            return balanceService.getWalletBalance(walletId);
        } catch (BalanceException e) {
            throw WalletException.balanceError("Failed to get balance: " + e.getMessage(), e);
        }
    }

    /**
     * Forces a refresh of the balance for a wallet.
     * 
     * @param walletId Wallet ID
     * @return Updated WalletBalance object
     * @throws WalletException If balance cannot be refreshed
     */
    public WalletBalance refreshWalletBalance(String walletId) throws WalletException {
        if (balanceService == null) {
            throw WalletException.balanceError("Balance service not available");
        }
        
        try {
            return balanceService.refreshWalletBalance(walletId);
        } catch (BalanceException e) {
            throw WalletException.balanceError("Failed to refresh balance: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a wallet has sufficient funds for a transaction.
     * 
     * @param walletId Wallet ID
     * @param amount Amount required (in satoshis)
     * @return true if sufficient funds, false otherwise
     * @throws WalletException If balance cannot be checked
     */
    public boolean hasSufficientFunds(String walletId, Coin amount) throws WalletException {
        if (balanceService == null) {
            return true; // If no balance service, assume sufficient funds (simulation mode)
        }
        
        try {
            return balanceService.hasSufficientFunds(walletId, amount);
        } catch (BalanceException e) {
            throw WalletException.balanceError("Failed to check funds: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the total balance across all wallets.
     * 
     * @return Total balance across all wallets
     * @throws WalletException If balance cannot be retrieved
     */
    public Coin getTotalBalance() throws WalletException {
        if (balanceService == null) {
            throw WalletException.balanceError("Balance service not available");
        }
        
        try {
            return balanceService.getTotalBalance();
        } catch (BalanceException e) {
            throw WalletException.balanceError("Failed to get total balance: " + e.getMessage(), e);
        }
    }

    /**
     * Gets all wallet IDs.
     * 
     * @return List of wallet IDs
     */
    public List<String> listWalletIds() {
        return new ArrayList<>(walletStorage.keySet());
    }
}