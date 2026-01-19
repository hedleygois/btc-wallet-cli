package com.btcwallet;

import com.btcwallet.cli.WalletCLI;
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
            // Initialize wallet service (default to MainNet)
            WalletService walletService = new WalletService();
            NetworkMonitor networkMonitor = new NetworkMonitor();
            FeeCalculator feeCalculator = new FeeCalculator(networkMonitor);
            TransactionService transactionService = new TransactionService(walletService, feeCalculator, networkMonitor);
            
            // Start the CLI interface
            WalletCLI cli = new WalletCLI(walletService, transactionService, feeCalculator, networkMonitor);
            cli.start();
            
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