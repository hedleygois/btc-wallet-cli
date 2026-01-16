package com.btcwallet.cli;

import com.btcwallet.exception.WalletException;
import com.btcwallet.model.Wallet;
import com.btcwallet.service.WalletGenerator;
import com.btcwallet.service.WalletService;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Command Line Interface for the BTC Wallet Management App.
 * Provides a simple text-based menu system for wallet operations.
 */
public class WalletCLI {
    
    private final WalletService walletService;
    private Scanner scanner;
    private boolean running;
    
    /**
     * Creates a new WalletCLI instance.
     *
     * @param walletService Wallet service to use for operations
     */
    public WalletCLI(WalletService walletService) {
        this.walletService = walletService;
        try {
            this.scanner = new Scanner(System.in);
        } catch (Exception e) {
            this.scanner = new Scanner(""); // Fallback to empty scanner if System.in not available
        }
        this.running = false;
    }
    
    public void start() {
    running = true;
    System.out.println("🔐 Bitcoin Wallet Manager");
    System.out.println("Network: " + walletService.getNetworkName());
    System.out.println("Type 'help' for available commands or 'exit' to quit.\n");
    
    while (running) {
        try {
            System.out.print("> ");
            System.out.flush();
            
            // Read the input line directly (blocks until user enters input)
            String input = scanner.nextLine().trim();
            
            // Skip empty lines
            if (input.isEmpty()) {
                continue;
            }
            
            // Process the command
            processCommand(input);
            
        } catch (WalletException e) {
            System.err.println("❌ " + e.getMessage());
        } catch (NoSuchElementException e) {
            System.err.println("❌ Input stream closed. Exiting...");
            running = false;
        } catch (IllegalStateException e) {
            System.err.println("❌ Scanner closed. Exiting...");
            running = false;
        } catch (Exception e) {
            System.err.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    System.out.println("\n👋 Thank you for using BTC Wallet Manager!");
    scanner.close();
}

    
    /**
     * Processes user commands.
     *
     * @param command User input command
     */
    private void processCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1].trim() : "";
        
        switch (cmd) {
            case "help":
            case "?":
                showHelp();
                break;
                
            case "generate":
            case "new":
                handleGenerateWallet();
                break;
                
            case "generate-mnemonic":
            case "new-mnemonic":
                handleGenerateWalletWithMnemonic();
                break;
                
            case "import":
                handleImportWallet(args);
                break;
                
            case "validate":
            case "check":
                handleValidateAddress(args);
                break;
                
            case "info":
            case "network":
                showNetworkInfo();
                break;
                
            case "exit":
            case "quit":
            case "q":
                running = false;
                break;
                
            default:
                System.out.println("❓ Unknown command. Type 'help' for available commands.");
        }
    }
    
    /**
     * Shows help information.
     */
    private void showHelp() {
        System.out.println("\n📖 Available Commands:");
        System.out.println("  help, ?                  - Show this help message");
        System.out.println("  generate, new            - Generate a new random wallet");
        System.out.println("  generate-mnemonic        - Generate wallet with mnemonic seed phrase");
        System.out.println("  import <key|mnemonic|wif> - Import wallet from private key, mnemonic, or WIF");
        System.out.println("  validate <address>       - Validate a Bitcoin address");
        System.out.println("  info, network            - Show network information");
        System.out.println("  exit, quit, q            - Exit the application");
        System.out.println();
        
        System.out.println("💡 Examples:");
        System.out.println("  generate                  - Create a new wallet");
        System.out.println("  import 5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF");
        System.out.println("  import 'abandon abandon...'");
        System.out.println("  validate 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa");
        System.out.println();
    }
    
    /**
     * Handles wallet generation.
     */
    private void handleGenerateWallet() {
        System.out.println("\n🔄 Generating new Bitcoin wallet...");
        
        Wallet wallet = walletService.generateWallet();
        
        System.out.println("✅ Wallet generated successfully!");
        System.out.println("🆔 Wallet ID: " + wallet.walletId());
        System.out.println("🏦 Address: " + wallet.address());
        System.out.println("🔑 Public Key: " + wallet.publicKey());
        System.out.println("🔒 Private Key: " + wallet.privateKey());
        System.out.println("📅 Created: " + wallet.createdAt());
        System.out.println("🌐 Network: " + walletService.getNetworkName());
        System.out.println();
        
        System.out.println("⚠️  IMPORTANT SECURITY NOTICE:");
        System.out.println("   - Never share your private key with anyone!");
        System.out.println("   - Anyone with your private key can access your funds!");
        System.out.println("   - Store this information securely offline.");
        System.out.println();
    }
    
    /**
     * Handles wallet generation with mnemonic.
     */
    private void handleGenerateWalletWithMnemonic() {
        System.out.println("\n🔄 Generating new Bitcoin wallet with mnemonic seed phrase...");
        
        WalletGenerator.WalletGenerationResult result = walletService.generateWalletWithMnemonic();
        Wallet wallet = result.getWallet();
        
        System.out.println("✅ Wallet generated successfully!");
        System.out.println("🆔 Wallet ID: " + wallet.walletId());
        System.out.println("🏦 Address: " + wallet.address());
        System.out.println("🔑 Public Key: " + wallet.publicKey());
        System.out.println("🔒 Private Key: " + wallet.privateKey());
        System.out.println("📅 Created: " + wallet.createdAt());
        System.out.println("🌐 Network: " + walletService.getNetworkName());
        System.out.println();
        
        System.out.println("📝 SEED PHRASE (12 words - write this down!):");
        System.out.println("   " + result.getMnemonic());
        System.out.println();
        
        System.out.println("⚠️  IMPORTANT SECURITY NOTICE:");
        System.out.println("   - This seed phrase can be used to recover your wallet!");
        System.out.println("   - Never share it with anyone!");
        System.out.println("   - Write it down on paper and store it securely offline.");
        System.out.println("   - Anyone with this seed phrase can access your funds!");
        System.out.println();
    }
    
    /**
     * Handles wallet import.
     *
     * @param args Import arguments
     */
    private void handleImportWallet(String args) {
        if (args.isEmpty()) {
            System.out.println("❓ Please specify what to import. Usage:");
            System.out.println("   import <private-key|mnemonic|wif>");
            System.out.println("   Example: import 5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF");
            System.out.println("   Example: import 'abandon abandon abandon...'");
            return;
        }
        
        try {
            System.out.println("\n🔄 Importing wallet...");
            
            Wallet wallet;
            String input = args.trim();
            
            // Try to detect the input type
            if (isValidHexPrivateKey(input)) {
                wallet = walletService.importFromPrivateKey(input);
                System.out.println("📥 Imported from private key (hex format)");
            } else if (isValidWIF(input)) {
                try {
                    wallet = walletService.importFromWIF(input);
                    System.out.println("📥 Imported from WIF format");
                } catch (WalletException e) {
                    if (e.getMessage().contains("not fully implemented")) {
                        System.out.println("ℹ️  WIF import is not fully implemented in this version.");
                        System.out.println("   Please use hex private key or mnemonic import instead.");
                        return;
                    } else {
                        throw e;
                    }
                }
            } else if (isValidMnemonic(input)) {
                wallet = walletService.importFromMnemonic(input);
                System.out.println("📥 Imported from mnemonic seed phrase");
            } else {
                System.out.println("❌ Could not determine input format. Please specify the format:");
                System.out.println("   import private:<key>    - For hex private key");
                System.out.println("   import mnemonic:<words> - For seed phrase");
                System.out.println("   import wif:<key>       - For WIF format");
                return;
            }
            
            System.out.println("✅ Wallet imported successfully!");
            System.out.println("🆔 Wallet ID: " + wallet.walletId());
            System.out.println("🏦 Address: " + wallet.address());
            System.out.println("🔑 Public Key: " + wallet.publicKey());
            System.out.println("🔒 Private Key: " + wallet.privateKey());
            System.out.println("🌐 Network: " + walletService.getNetworkName());
            System.out.println();
            
        } catch (WalletException e) {
            throw e; // Will be caught by the main loop
        }
    }
    
    /**
     * Handles address validation.
     *
     * @param address Bitcoin address to validate
     */
    private void handleValidateAddress(String address) {
        if (address.isEmpty()) {
            System.out.println("❓ Please provide an address to validate.");
            System.out.println("   Example: validate 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa");
            return;
        }
        
        boolean isValid = walletService.isValidAddress(address);
        
        if (isValid) {
            System.out.println("✅ Valid Bitcoin address: " + address);
        } else {
            System.out.println("❌ Invalid Bitcoin address: " + address);
        }
        System.out.println();
    }
    
    /**
     * Shows network information.
     */
    private void showNetworkInfo() {
        System.out.println("\n🌐 Network Information:");
        System.out.println("   Network: " + walletService.getNetworkName());
        System.out.println("   BitcoinJ Version: " + org.bitcoinj.core.VersionMessage.BITCOINJ_VERSION);
        System.out.println();
    }
    
    /**
     * Simple heuristic to check if input looks like a hex private key.
     */
    private boolean isValidHexPrivateKey(String input) {
        // Hex private keys are typically 64 characters long (32 bytes)
        return input.length() == 64 && input.matches("^[0-9a-fA-F]+$");
    }
    
    /**
     * Simple heuristic to check if input looks like a WIF private key.
     */
    private boolean isValidWIF(String input) {
        // WIF keys typically start with 5 (MainNet) or 9/ c (TestNet) and are 51-52 characters
        return (input.length() == 51 || input.length() == 52) && 
               (input.startsWith("5") || input.startsWith("9") || input.startsWith("c") || input.startsWith("K") || input.startsWith("L"));
    }
    
    /**
     * Simple heuristic to check if input looks like a mnemonic.
     */
    private boolean isValidMnemonic(String input) {
        // Mnemonics are typically 12, 18, or 24 words separated by spaces
        String[] words = input.split("\\s+");
        return words.length == 12 || words.length == 18 || words.length == 24;
    }
}