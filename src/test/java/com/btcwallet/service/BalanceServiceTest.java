package com.btcwallet.service;

import com.btcwallet.exception.BalanceException;
import com.btcwallet.model.Wallet;
import com.btcwallet.model.WalletBalance;
import org.bitcoinj.core.Coin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BalanceServiceTest {

    @Mock
    private BitcoinNodeClient bitcoinNodeClient;

    @Mock
    private WalletService walletService;

    private BalanceService balanceService;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        balanceService = new BalanceService(bitcoinNodeClient, walletService);
    }

    @AfterEach
    void tearDown() throws Exception {
        balanceService.shutdown();
        closeable.close();
    }

    @Test
    void testGetWalletBalance_CacheMiss() throws Exception {
        String walletId = "W1";
        Wallet wallet = mock(Wallet.class);
        WalletBalance balance = new WalletBalance(walletId, Coin.COIN, Coin.ZERO, Coin.COIN, Instant.now(), "100", Collections.emptyList());

        when(walletService.getWallet(walletId)).thenReturn(wallet);
        when(bitcoinNodeClient.getWalletBalance(wallet)).thenReturn(balance);

        WalletBalance result = balanceService.getWalletBalance(walletId);

        assertEquals(balance, result);
        verify(bitcoinNodeClient, times(1)).getWalletBalance(wallet);
    }

    @Test
    void testGetWalletBalance_CacheHit() throws Exception {
        String walletId = "W1";
        Wallet wallet = mock(Wallet.class);
        WalletBalance balance = new WalletBalance(walletId, Coin.COIN, Coin.ZERO, Coin.COIN, Instant.now(), "100", Collections.emptyList());

        when(walletService.getWallet(walletId)).thenReturn(wallet);
        when(bitcoinNodeClient.getWalletBalance(wallet)).thenReturn(balance);

        // First call - cache miss
        balanceService.getWalletBalance(walletId);
        
        // Second call - cache hit
        WalletBalance result = balanceService.getWalletBalance(walletId);

        assertEquals(balance, result);
        verify(bitcoinNodeClient, times(1)).getWalletBalance(wallet);
    }

    @Test
    void testRefreshWalletBalance() throws Exception {
        String walletId = "W1";
        Wallet wallet = mock(Wallet.class);
        WalletBalance balance1 = new WalletBalance(walletId, Coin.COIN, Coin.ZERO, Coin.COIN, Instant.now(), "100", Collections.emptyList());
        WalletBalance balance2 = new WalletBalance(walletId, Coin.valueOf(2, 0), Coin.ZERO, Coin.valueOf(2, 0), Instant.now(), "101", Collections.emptyList());

        when(walletService.getWallet(walletId)).thenReturn(wallet);
        when(bitcoinNodeClient.getWalletBalance(wallet)).thenReturn(balance1).thenReturn(balance2);

        balanceService.getWalletBalance(walletId);
        WalletBalance result = balanceService.refreshWalletBalance(walletId);

        assertEquals(balance2, result);
        verify(bitcoinNodeClient, times(2)).getWalletBalance(wallet);
    }

    @Test
    void testGetWalletBalance_WalletNotFound() {
        String walletId = "Unknown";
        when(walletService.getWallet(walletId)).thenReturn(null);

        assertThrows(BalanceException.class, () -> balanceService.getWalletBalance(walletId));
    }

    @Test
    void testRefreshAllBalances() throws Exception {
        String w1 = "W1";
        String w2 = "W2";
        Wallet wallet1 = mock(Wallet.class);
        Wallet wallet2 = mock(Wallet.class);
        WalletBalance b1 = new WalletBalance(w1, Coin.COIN, Coin.ZERO, Coin.COIN, Instant.now(), "100", Collections.emptyList());
        WalletBalance b2 = new WalletBalance(w2, Coin.FIFTY_COINS, Coin.ZERO, Coin.FIFTY_COINS, Instant.now(), "100", Collections.emptyList());

        when(walletService.listWalletIds()).thenReturn(List.of(w1, w2));
        when(walletService.getWallet(w1)).thenReturn(wallet1);
        when(walletService.getWallet(w2)).thenReturn(wallet2);
        when(bitcoinNodeClient.getWalletBalance(wallet1)).thenReturn(b1);
        when(bitcoinNodeClient.getWalletBalance(wallet2)).thenReturn(b2);

        balanceService.refreshAllBalances();

        verify(bitcoinNodeClient).getWalletBalance(wallet1);
        verify(bitcoinNodeClient).getWalletBalance(wallet2);
    }

    @Test
    void testHasSufficientFunds() throws Exception {
        String walletId = "W1";
        Wallet wallet = mock(Wallet.class);
        // 1 BTC total balance
        WalletBalance balance = new WalletBalance(walletId, Coin.COIN, Coin.ZERO, Coin.COIN, Instant.now(), "100", Collections.emptyList());

        when(walletService.getWallet(walletId)).thenReturn(wallet);
        when(bitcoinNodeClient.getWalletBalance(wallet)).thenReturn(balance);

        // 0.5 BTC is sufficient
        assertTrue(balanceService.hasSufficientFunds(walletId, Coin.valueOf(50000000)));
        // 2.0 BTC is not sufficient
        assertFalse(balanceService.hasSufficientFunds(walletId, Coin.valueOf(200000000)));
    }

    @Test
    void testHasSufficientConfirmedFunds() throws Exception {
        String walletId = "W1";
        Wallet wallet = mock(Wallet.class);
        // 0.5 confirmed, 0.5 unconfirmed = 1.0 total
        Coin halfCoin = Coin.valueOf(50000000);
        WalletBalance balance = new WalletBalance(walletId, halfCoin, halfCoin, Coin.COIN, Instant.now(), "100", Collections.emptyList());

        when(walletService.getWallet(walletId)).thenReturn(wallet);
        when(bitcoinNodeClient.getWalletBalance(wallet)).thenReturn(balance);

        // 0.25 BTC is sufficient (confirmed is 0.5)
        assertTrue(balanceService.hasSufficientConfirmedFunds(walletId, Coin.valueOf(25000000)));
        // 1.0 BTC is not sufficient (confirmed is only 0.5)
        assertFalse(balanceService.hasSufficientConfirmedFunds(walletId, Coin.COIN));
    }

    @Test
    void testGetTotalBalance() throws Exception {
        String w1 = "W1";
        String w2 = "W2";
        Wallet wallet1 = mock(Wallet.class);
        Wallet wallet2 = mock(Wallet.class);
        WalletBalance b1 = new WalletBalance(w1, Coin.COIN, Coin.ZERO, Coin.COIN, Instant.now(), "100", Collections.emptyList());
        WalletBalance b2 = new WalletBalance(w2, Coin.FIFTY_COINS, Coin.ZERO, Coin.FIFTY_COINS, Instant.now(), "100", Collections.emptyList());

        when(walletService.listWalletIds()).thenReturn(List.of(w1, w2));
        when(walletService.getWallet(w1)).thenReturn(wallet1);
        when(walletService.getWallet(w2)).thenReturn(wallet2);
        when(bitcoinNodeClient.getWalletBalance(wallet1)).thenReturn(b1);
        when(bitcoinNodeClient.getWalletBalance(wallet2)).thenReturn(b2);

        Coin total = balanceService.getTotalBalance();

        assertEquals(Coin.COIN.multiply(51), total);
    }

    @Test
    void testClearBalanceCache() throws Exception {
        String walletId = "W1";
        Wallet wallet = mock(Wallet.class);
        WalletBalance balance = new WalletBalance(walletId, Coin.COIN, Coin.ZERO, Coin.COIN, Instant.now(), "100", Collections.emptyList());

        when(walletService.getWallet(walletId)).thenReturn(wallet);
        when(bitcoinNodeClient.getWalletBalance(wallet)).thenReturn(balance);

        balanceService.getWalletBalance(walletId); // Populates cache
        balanceService.clearBalanceCache(walletId);
        
        balanceService.getWalletBalance(walletId); // Should trigger another fetch

        verify(bitcoinNodeClient, times(2)).getWalletBalance(wallet);
    }
}
