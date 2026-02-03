package com.btcwallet;

import com.btcwallet.cli.WalletCLI;
import com.btcwallet.config.BitcoinConfig;
import com.btcwallet.exception.BitcoinConfigurationException;
import com.btcwallet.exception.WalletException;
import com.btcwallet.service.*;

/**
 * Main entry point for the BTC Wallet Management Application.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== BTC Wallet Management App ===");
        System.out.println("A simple Bitcoin wallet manager using BitcoinJ");
        System.out.println("======================================\n");
        
        try {
            // Load Bitcoin configuration
            BitcoinConfig bitcoinConfig = new BitcoinConfig();
            System.out.println("📖 Loaded Bitcoin configuration: " + bitcoinConfig);
            
            // Initialize Bitcoin node client
            BitcoinNodeClient bitcoinNodeClient = new BitcoinNodeClient(bitcoinConfig);
            
            // Initialize wallet service (default to MainNet)
            WalletService walletService = new WalletService();
            NetworkMonitor networkMonitor = new NetworkMonitor();
            FeeCalculator feeCalculator = new FeeCalculator(networkMonitor);
            
            // Initialize balance service
            BalanceService balanceService = new BalanceService(bitcoinNodeClient, walletService);
            walletService.setBalanceService(balanceService);
            
            // Initialize transaction service with Bitcoin node client and balance service
            TransactionService transactionService = new TransactionService(
                walletService, feeCalculator, networkMonitor, bitcoinNodeClient, balanceService);

            // Start the CLI interface
            WalletCLI cli = new WalletCLI(walletService, transactionService, feeCalculator, networkMonitor, balanceService);
            cli.start();
            
        } catch (BitcoinConfigurationException e) {
            System.err.println("\n[CONFIG ERROR] " + e.getMessage());
            System.err.println("Please check your bitcoin.properties file.");
            System.exit(1);
        } catch (WalletException e) {
            System.err.println("\n[FATAL ERROR] " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("\n[UNEXPECTED ERROR] An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}