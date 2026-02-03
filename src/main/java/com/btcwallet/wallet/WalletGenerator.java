package com.btcwallet.wallet;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Service for generating new Bitcoin wallets.
 */
public class WalletGenerator {
    
    private final NetworkParameters networkParameters;
    private final SecureRandom secureRandom;
    
    /**
     * Creates a new WalletGenerator instance.
     *
     * @param networkParameters Network parameters (MainNet, TestNet, etc.)
     */
    public WalletGenerator(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Generates a new Bitcoin wallet with a random key pair.
     *
     * @return Newly generated Wallet
     */
    public Wallet generateWallet() {
        ECKey ecKey = new ECKey(secureRandom);
        String walletId = generateWalletId();
        return Wallet.fromECKey(walletId, ecKey, networkParameters);
    }
    
    /**
     * Generates a new Bitcoin wallet with a mnemonic seed phrase.
     *
     * @return WalletGenerationResult containing the wallet and mnemonic
     */
    public WalletGenerationResult generateWalletWithMnemonic() {
        byte[] entropy = new byte[16]; // 128 bits for 12-word mnemonic
        secureRandom.nextBytes(entropy);
        
        try {
            MnemonicCode mnemonicCode = MnemonicCode.INSTANCE;
            List<String> mnemonicWords = mnemonicCode.toMnemonic(entropy);
            byte[] seed = MnemonicCode.toSeed(mnemonicWords, ""); // Empty passphrase
            
            // BIP44 Path: m / 44' / coin_type' / account' / change / address_index
            
            // 1. Master Key (m)
            DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed);
            
            // 2. Purpose (44') - BIP44 standard for HD wallets
            DeterministicKey purposeKey = HDKeyDerivation.deriveChildKey(masterKey, new ChildNumber(44, true));
            
            // 3. Coin Type - 0' for MainNet, 1' for TestNet
            int coinType = networkParameters.equals(MainNetParams.get()) ? 0 : 1;
            DeterministicKey coinTypeKey = HDKeyDerivation.deriveChildKey(purposeKey, new ChildNumber(coinType, true));
            
            // 4. Account (0') - First account
            DeterministicKey accountKey = HDKeyDerivation.deriveChildKey(coinTypeKey, new ChildNumber(0, true));
            
            // 5. Change (0) - 0 for external (receiving) chain
            DeterministicKey changeKey = HDKeyDerivation.deriveChildKey(accountKey, new ChildNumber(0, false));
            
            // 6. Address Index (0) - The first address in the chain
            DeterministicKey addressKey = HDKeyDerivation.deriveChildKey(changeKey, new ChildNumber(0, false));

            ECKey ecKey = ECKey.fromPrivate(addressKey.getPrivKeyBytes());
            String walletId = generateWalletId();
            Wallet wallet = Wallet.fromECKey(walletId, ecKey, networkParameters);
            
            return new WalletGenerationResult(wallet, String.join(" ", mnemonicWords));
        } catch (Exception e) {
            throw WalletException.generationFailed("Failed to generate mnemonic seed phrase: " + e.getMessage());
        }
    }
    
    /**
     * Generates a unique wallet ID.
     *
     * @return Unique wallet identifier
     */
    private String generateWalletId() {
        return "WALLET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Result object for wallet generation with mnemonic.
     */
    public static class WalletGenerationResult {
        private final Wallet wallet;
        private final String mnemonic;
        
        public WalletGenerationResult(Wallet wallet, String mnemonic) {
            this.wallet = wallet;
            this.mnemonic = mnemonic;
        }
        
        public Wallet getWallet() {
            return wallet;
        }
        
        public String getMnemonic() {
            return mnemonic;
        }
    }
    

}