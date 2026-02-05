package com.btcwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
    public WalletService walletService(BitcoinConfig config, BitcoinNodeClient bitcoinNodeClient) {
        return new WalletService(config.getNetworkParameters(), bitcoinNodeClient);
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
    public TransactionService transactionService(
            WalletService walletService,
            FeeCalculator feeCalculator,
            NetworkMonitor networkMonitor,
            BitcoinNodeClient bitcoinNodeClient) {
        return new TransactionService(walletService, feeCalculator, networkMonitor, bitcoinNodeClient);
    }
}