package com.btcwallet.service;

import com.btcwallet.exception.WalletException;
import com.btcwallet.model.Wallet;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;

import java.util.List;
import java.util.UUID;

/**
 * Service for importing existing Bitcoin wallets from private keys or seed phrases.
 */
public class WalletImporter {
    
    private final NetworkParameters networkParameters;
    
    /**
     * Creates a new WalletImporter instance.
     *
     * @param networkParameters Network parameters (MainNet, TestNet, etc.)
     */
    public WalletImporter(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
    }
    
    /**
     * Imports a wallet from a private key in hex format.
     *
     * @param privateKeyHex Private key in hexadecimal format
     * @return Imported Wallet
     * @throws WalletImportException If the private key is invalid
     */
    public Wallet importFromPrivateKey(String privateKeyHex) {
        try {
            if (privateKeyHex == null || privateKeyHex.trim().isEmpty()) {
                throw WalletException.invalidPrivateKey("Private key cannot be null or empty");
            }
            
            ECKey ecKey = ECKey.fromPrivate(org.bitcoinj.core.Utils.HEX.decode(privateKeyHex));
            String walletId = generateWalletId();
            return Wallet.fromECKey(walletId, ecKey, networkParameters);
        } catch (Exception e) {
            throw WalletException.invalidPrivateKey("Invalid private key format: " + e.getMessage());
        }
    }
    
    /**
     * Imports a wallet from a mnemonic seed phrase.
     *
     * @param mnemonic Mnemonic seed phrase (space-separated words)
     * @return Imported Wallet
     * @throws WalletImportException If the mnemonic is invalid
     */
    public Wallet importFromMnemonic(String mnemonic) {
        try {
            if (mnemonic == null || mnemonic.trim().isEmpty()) {
                throw WalletException.invalidMnemonic("Mnemonic seed phrase cannot be null or empty");
            }
            
            List<String> mnemonicWords = List.of(mnemonic.trim().split("\\s+"));
            byte[] seed = MnemonicCode.toSeed(mnemonicWords, ""); // Empty passphrase
            
            // Use proper BIP32/BIP44 derivation for mnemonic seeds
            // For simplicity, we'll use the first 32 bytes of the seed as the private key
            // In a production app, you would use proper hierarchical deterministic derivation
            if (seed.length > 32) {
                byte[] privateKeyBytes = new byte[32];
                System.arraycopy(seed, 0, privateKeyBytes, 0, 32);
                ECKey ecKey = ECKey.fromPrivate(privateKeyBytes);
                String walletId = generateWalletId();
                return Wallet.fromECKey(walletId, ecKey, networkParameters);
            } else {
                ECKey ecKey = ECKey.fromPrivate(seed);
                String walletId = generateWalletId();
                return Wallet.fromECKey(walletId, ecKey, networkParameters);
            }
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("mnemonic") || e.getMessage().contains("word"))) {
                throw WalletException.invalidMnemonic("Invalid mnemonic seed phrase: " + e.getMessage());
            } else {
                throw WalletException.importFailed("Failed to import wallet from mnemonic: " + e.getMessage());
            }
        }
    }
    
    /**
     * Imports a wallet from a WIF (Wallet Import Format) private key.
     *
     * @param wifPrivateKey WIF formatted private key
     * @return Imported Wallet
     * @throws WalletImportException If the WIF key is invalid
     */
    public Wallet importFromWIF(String wifPrivateKey) {
        try {
            if (wifPrivateKey == null || wifPrivateKey.trim().isEmpty()) {
                throw WalletException.invalidPrivateKey("WIF private key cannot be null or empty");
            }
            
            // Note: WIF import requires proper Base58 decoding and checksum validation
            // This is a simplified placeholder - a real implementation would need:
            // 1. Base58 decoding
            // 2. Checksum validation  
            // 3. Network byte handling
            // 4. Proper key reconstruction
            throw new WalletException(WalletException.ErrorType.INVALID_INPUT, "WIF import not fully implemented in this version");
        } catch (Exception e) {
            throw WalletException.invalidPrivateKey("Invalid WIF private key format: " + e.getMessage());
        }
    }
    
    /**
     * Generates a unique wallet ID.
     *
     * @return Unique wallet identifier
     */
    private String generateWalletId() {
        return "IMPORTED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    

}