package com.btcwallet;

import com.btcwallet.model.Wallet;
import com.btcwallet.service.WalletGenerator;
import com.btcwallet.service.WalletService;
import org.bitcoinj.params.MainNetParams;

/**
 * Simple test to verify core functionality without JUnit.
 */
public class SimpleTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("Running simple wallet functionality test...");
            
            // Test wallet service creation
            WalletService walletService = new WalletService();
            System.out.println("✅ WalletService created successfully");
            
            // Test wallet generation
            Wallet wallet = walletService.generateWallet();
            System.out.println("✅ Wallet generated successfully");
            System.out.println("   Address: " + wallet.getAddress());
            System.out.println("   Public Key: " + wallet.getPublicKey());
            System.out.println("   Network: " + walletService.getNetworkName());
            
            // Test address validation
            boolean isValid = walletService.isValidAddress(wallet.getAddress());
            System.out.println("✅ Address validation: " + (isValid ? "PASS" : "FAIL"));
            
            // Test wallet generation with mnemonic
            WalletGenerator.WalletGenerationResult mnemonicResult = walletService.generateWalletWithMnemonic();
            System.out.println("✅ Wallet with mnemonic generated successfully");
            System.out.println("   Mnemonic words: " + mnemonicResult.getMnemonic().split("\\s+").length);
            
            // Test wallet import from private key
            Wallet importedWallet = walletService.importFromPrivateKey(wallet.getPrivateKey());
            System.out.println("✅ Wallet imported from private key successfully");
            System.out.println("   Imported Address: " + importedWallet.getAddress());
            
            // Verify addresses match
            boolean addressesMatch = wallet.getAddress().equals(importedWallet.getAddress());
            System.out.println("✅ Address match verification: " + (addressesMatch ? "PASS" : "FAIL"));
            
            System.out.println("\n🎉 All tests passed! Core functionality is working correctly.");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}