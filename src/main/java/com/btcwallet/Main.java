package com.btcwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.btcwallet.balance.BalanceService;
import com.btcwallet.config.BitcoinConfig;
import com.btcwallet.network.BitcoinNodeClient;
import com.btcwallet.network.FeeCalculator;
import com.btcwallet.network.NetworkMonitor;
import com.btcwallet.transaction.TransactionService;
import com.btcwallet.wallet.WalletService;

@SpringBootApplication
public class Main {
    
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public BitcoinConfig bitcoinConfig() throws com.btcwallet.exception.BitcoinConfigurationException {
        return new BitcoinConfig();
    }

    @Bean
    public BitcoinNodeClient bitcoinNodeClient(BitcoinConfig bitcoinConfig) {
        return new BitcoinNodeClient(bitcoinConfig);
    }

    @Bean
    public WalletService walletService() {
        // WalletService might need BalanceService, set via setter to avoid circular dependency
        return new WalletService();
    }

    @Bean
    public NetworkMonitor networkMonitor() {
        return new NetworkMonitor();
    }

    @Bean
    public FeeCalculator feeCalculator(NetworkMonitor networkMonitor) {
        return new FeeCalculator(networkMonitor);
    }

    @Bean
    public BalanceService balanceService(BitcoinNodeClient bitcoinNodeClient, WalletService walletService) {
        BalanceService service = new BalanceService(bitcoinNodeClient, walletService);
        walletService.setBalanceService(service); // Set balance service on wallet service
        return service;
    }

    @Bean
    public TransactionService transactionService(
            WalletService walletService,
            FeeCalculator feeCalculator,
            NetworkMonitor networkMonitor,
            BitcoinNodeClient bitcoinNodeClient,
            BalanceService balanceService) {
        return new TransactionService(walletService, feeCalculator, networkMonitor, bitcoinNodeClient, balanceService);
    }
}