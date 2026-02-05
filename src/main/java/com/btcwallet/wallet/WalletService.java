package com.btcwallet.wallet;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;

import com.btcwallet.balance.BalanceCache;
import com.btcwallet.balance.BalanceException;
import com.btcwallet.balance.WalletBalance;
import com.btcwallet.network.BitcoinNodeClient;

public class WalletService {

    private final WalletGenerator walletGenerator;
    private final WalletImporter walletImporter;
    private final NetworkParameters networkParameters;
    private final Map<String, Wallet> walletStorage = new HashMap<>();
    private final BitcoinNodeClient bitcoinNodeClient;
    private final BalanceCache balanceCache = BalanceCache.getInstance();
    private final ScheduledExecutorService refreshScheduler;

    /**
     * Creates a new WalletService instance.
     *
     * @param networkParameters Network parameters to use
     * @param bitcoinNodeClient Bitcoin node client for blockchain operations
     */
    public WalletService(NetworkParameters networkParameters, BitcoinNodeClient bitcoinNodeClient) {
        this.networkParameters = networkParameters;
        this.walletGenerator = new WalletGenerator(networkParameters);
        this.walletImporter = new WalletImporter(networkParameters);
        this.bitcoinNodeClient = bitcoinNodeClient;
        this.refreshScheduler = Executors.newSingleThreadScheduledExecutor();
        
        startBackgroundRefresh();
    }

    /**
     * Starts the background balance refresh scheduler.
     * Very simple refresher - DEFINITELY not production ready :) 
     */
    private void startBackgroundRefresh() {
        refreshScheduler.scheduleAtFixedRate(() -> {
            try {
                refreshAllBalances();
            } catch (Exception e) {
                System.err.println("Background balance refresh failed: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    private void refreshAllBalances() throws BalanceException {
        for (String walletId : listWalletIds()) {
            refreshWalletBalance(walletId);
        }
    }

    /**
     * Shuts down the wallet service and cleans up resources.
     */
    public void shutdown() {
        if (refreshScheduler != null) {
            refreshScheduler.shutdown();
            try {
                if (!refreshScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    refreshScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                refreshScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
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
        WalletBalance cached = balanceCache.get(walletId);
        if (cached != null) {
            return cached;
        }
        return refreshWalletBalance(walletId);
    }

    /**
     * Forces a refresh of the balance for a wallet.
     * 
     * @param walletId Wallet ID
     * @return Updated WalletBalance object
     * @throws WalletException If balance cannot be refreshed
     */
    public WalletBalance refreshWalletBalance(String walletId) throws WalletException {
        try {
            Wallet wallet = getWallet(walletId);
            if (wallet == null) {
                throw WalletException.balanceError("Wallet not found: " + walletId);
            }

            if (bitcoinNodeClient == null) {
                // Return a dummy balance if no client (simulation mode)
                return new WalletBalance(walletId, Coin.ZERO, Coin.ZERO, Coin.ZERO, Instant.now(), "0", List.of());
            }

            WalletBalance balance = bitcoinNodeClient.getWalletBalance(wallet);
            balanceCache.put(walletId, balance);
            return balance;
        } catch (Exception e) {
            throw WalletException.balanceError("Failed to refresh balance: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a wallet has sufficient funds for a transaction.
     * 
     * @param walletId Wallet ID
     * @param amount   Amount required (in satoshis)
     * @return true if sufficient funds, false otherwise
     * @throws WalletException If balance cannot be checked
     */
    public boolean hasSufficientFunds(String walletId, Coin amount) throws WalletException {
        WalletBalance balance = getWalletBalance(walletId);
        return balance.hasSufficientFunds(amount);
    }

    /**
     * Gets the total balance across all wallets.
     * 
     * @return Total balance across all wallets
     * @throws WalletException If balance cannot be retrieved
     */
    public Coin getTotalBalance() throws WalletException {
        Coin total = Coin.ZERO;
        for (String walletId : listWalletIds()) {
            total = total.add(getWalletBalance(walletId).getTotalBalance());
        }
        return total;
    }

    /**
     * 
     * @return List of wallet IDs
     */
    public List<String> listWalletIds() {
        return new ArrayList<>(walletStorage.keySet());
    }

    /**
     * Simple heuristic to check if input looks like a hex private key.
     * 
     * @param input The string to check.
     * @return true if the input matches hex private key pattern.
     */
    public boolean isValidHexPrivateKey(String input) {
        if (input == null)
            return false;
        // Hex private keys are typically 64 characters long (32 bytes)
        return input.length() == 64 && input.matches("^[0-9a-fA-F]+$");
    }

    /**
     * Simple heuristic to check if input looks like a WIF private key.
     * 
     * @param input The string to check.
     * @return true if the input matches WIF private key pattern.
     */
    public boolean isValidWIF(String input) {
        if (input == null)
            return false;

        // WIF keys typically start with 5 (MainNet) or 9/ c (TestNet) and are 51-52
        // characters

        return (input.length() == 51 || input.length() == 52) &&

                (input.startsWith("5") || input.startsWith("9") || input.startsWith("c") || input.startsWith("K")
                        || input.startsWith("L"));

    }

    /**
     * Simple heuristic to check if input looks like a mnemonic.
     *
     * @param input The string to check.
     * @return true if the input matches mnemonic word count.
     */
    public boolean isValidMnemonic(String input) {

        if (input == null)
            return false;

        // Mnemonics are typically 12, 18, or 24 words separated by spaces
        String[] words = input.trim().split("\\s+");

        return (words.length == 12 || words.length == 18 || words.length == 24);

    }

}
