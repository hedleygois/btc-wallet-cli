package com.btcwallet.balance;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton cache for wallet balances.
 */
public class BalanceCache {
    private static BalanceCache instance;
    private final Map<String, WalletBalance> balanceCache = new ConcurrentHashMap<>();
    private final Map<String, Instant> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MINUTES = 5;

    private BalanceCache() {}

    public static synchronized BalanceCache getInstance() {
        if (instance == null) {
            instance = new BalanceCache();
        }
        return instance;
    }

    public WalletBalance get(String walletId) {
        if (isCacheStale(walletId)) {
            return null;
        }
        return balanceCache.get(walletId);
    }

    public void put(String walletId, WalletBalance balance) {
        balanceCache.put(walletId, balance);
        cacheTimestamps.put(walletId, Instant.now());
    }

    public boolean isCacheStale(String walletId) {
        if (!cacheTimestamps.containsKey(walletId)) {
            return true;
        }
        Instant lastUpdated = cacheTimestamps.get(walletId);
        return ChronoUnit.MINUTES.between(lastUpdated, Instant.now()) >= CACHE_TTL_MINUTES;
    }

    public void clear(String walletId) {
        balanceCache.remove(walletId);
        cacheTimestamps.remove(walletId);
    }

    public void clearAll() {
        balanceCache.clear();
        cacheTimestamps.clear();
    }

    public Map<String, WalletBalance> getAllCachedBalances() {
        return new ConcurrentHashMap<>(balanceCache);
    }
}
