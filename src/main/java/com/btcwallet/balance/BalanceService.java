package com.btcwallet.balance;

import com.btcwallet.network.BitcoinNodeClient;
import com.btcwallet.wallet.Wallet;
import com.btcwallet.wallet.WalletService;

import org.bitcoinj.core.Coin;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing and querying wallet balances.
 * Provides balance checking, caching, and monitoring capabilities.
 */
public class BalanceService {
    private final BitcoinNodeClient bitcoinNodeClient;
    private final WalletService walletService;
    private final Map<String, WalletBalance> balanceCache;
    private final Map<String, Instant> cacheTimestamps;
    private final ScheduledExecutorService refreshScheduler;
    private final long cacheTTLMinutes;

    /**
     * Creates a new BalanceService.
     *
     * @param bitcoinNodeClient Bitcoin node client for blockchain operations
     * @param walletService Wallet service for wallet operations
     */
    public BalanceService(BitcoinNodeClient bitcoinNodeClient, WalletService walletService) {
        this.bitcoinNodeClient = bitcoinNodeClient;
        this.walletService = walletService;
        this.balanceCache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
        this.cacheTTLMinutes = 5; // Default cache TTL: 5 minutes
        this.refreshScheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Start background refresh scheduler
        startBackgroundRefresh();
    }

    /**
     * Starts the background balance refresh scheduler.
     */
    private void startBackgroundRefresh() {
        // Refresh all cached balances every 5 minutes
        refreshScheduler.scheduleAtFixedRate(() -> {
            try {
                refreshAllBalances();
            } catch (BalanceException e) {
                System.err.println("Background balance refresh failed: " + e.getMessage());
            }
        }, cacheTTLMinutes, cacheTTLMinutes, TimeUnit.MINUTES);
    }

    /**
     * Gets the current balance for a wallet.
     * Uses cached value if fresh, otherwise fetches from blockchain.
     *
     * @param walletId Wallet ID
     * @return WalletBalance object
     * @throws BalanceException If balance cannot be retrieved
     */
    public WalletBalance getWalletBalance(String walletId) throws BalanceException {
        // Check if we have a fresh cached balance
        if (balanceCache.containsKey(walletId) && !isCacheStale(walletId)) {
            return balanceCache.get(walletId);
        }

        // Fetch fresh balance from blockchain
        return fetchAndCacheBalance(walletId);
    }

    /**
     * Forces a refresh of the balance for a wallet from the blockchain.
     *
     * @param walletId Wallet ID
     * @return Updated WalletBalance object
     * @throws BalanceException If balance cannot be retrieved
     */
    public WalletBalance refreshWalletBalance(String walletId) throws BalanceException {
        return fetchAndCacheBalance(walletId);
    }

    /**
     * Refreshes balances for all wallets.
     *
     * @throws BalanceException If any balance cannot be retrieved
     */
    public void refreshAllBalances() throws BalanceException {
        for (String walletId : walletService.listWalletIds()) {
            try {
                fetchAndCacheBalance(walletId);
            } catch (BalanceException e) {
                System.err.println("Failed to refresh balance for wallet " + walletId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Fetches balance from blockchain and caches it.
     *
     * @param walletId Wallet ID
     * @return WalletBalance object
     * @throws BalanceException If balance cannot be retrieved
     */
    private WalletBalance fetchAndCacheBalance(String walletId) throws BalanceException {
        try {
            Wallet wallet = walletService.getWallet(walletId);
            if (wallet == null) {
                throw BalanceException.balanceFetchFailed("Wallet not found: " + walletId);
            }

            // Use BitcoinNodeClient to fetch balance from blockchain
            WalletBalance balance = bitcoinNodeClient.getWalletBalance(wallet);
            
            // Cache the balance
            balanceCache.put(walletId, balance);
            cacheTimestamps.put(walletId, Instant.now());
            
            return balance;
        } catch (Exception e) {
            throw BalanceException.balanceFetchFailed(e.getMessage());
        }
    }

    /**
     * Checks if the cached balance for a wallet is stale.
     *
     * @param walletId Wallet ID
     * @return true if cache is stale, false if fresh
     */
    private boolean isCacheStale(String walletId) {
        if (!cacheTimestamps.containsKey(walletId)) {
            return true; // No cache entry exists
        }

        Instant lastUpdated = cacheTimestamps.get(walletId);
        Instant now = Instant.now();
        
        return ChronoUnit.MINUTES.between(lastUpdated, now) >= cacheTTLMinutes;
    }

    /**
     * Checks if a wallet has sufficient funds for a transaction.
     *
     * @param walletId Wallet ID
     * @param amount Amount required (in satoshis)
     * @return true if sufficient funds, false otherwise
     * @throws BalanceException If balance cannot be retrieved
     */
    public boolean hasSufficientFunds(String walletId, Coin amount) throws BalanceException {
        WalletBalance balance = getWalletBalance(walletId);
        return balance.hasSufficientFunds(amount);
    }

    /**
     * Checks if a wallet has sufficient confirmed funds for a transaction.
     *
     * @param walletId Wallet ID
     * @param amount Amount required (in satoshis)
     * @return true if sufficient confirmed funds, false otherwise
     * @throws BalanceException If balance cannot be retrieved
     */
    public boolean hasSufficientConfirmedFunds(String walletId, Coin amount) throws BalanceException {
        WalletBalance balance = getWalletBalance(walletId);
        return balance.hasSufficientConfirmedFunds(amount);
    }

    /**
     * Gets the total balance across all wallets.
     *
     * @return Total balance across all wallets
     * @throws BalanceException If any balance cannot be retrieved
     */
    public Coin getTotalBalance() throws BalanceException {
        Coin total = Coin.ZERO;
        
        for (String walletId : walletService.listWalletIds()) {
            WalletBalance balance = getWalletBalance(walletId);
            total = total.add(balance.getTotalBalance());
        }
        
        return total;
    }

    /**
     * Clears the balance cache for a specific wallet.
     *
     * @param walletId Wallet ID
     */
    public void clearBalanceCache(String walletId) {
        balanceCache.remove(walletId);
        cacheTimestamps.remove(walletId);
    }

    /**
     * Clears the entire balance cache.
     */
    public void clearAllBalanceCache() {
        balanceCache.clear();
        cacheTimestamps.clear();
    }

    /**
     * Shuts down the balance service and cleans up resources.
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
     * Gets the current cache TTL in minutes.
     *
     * @return Cache TTL in minutes
     */
    public long getCacheTTLMinutes() {
        return cacheTTLMinutes;
    }
}