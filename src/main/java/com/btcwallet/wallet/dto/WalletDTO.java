package com.btcwallet.wallet.dto;

import com.btcwallet.wallet.Wallet;

import java.time.Instant;

public record WalletDTO(
        String walletId,
        String address,
        String publicKey,
        Instant createdAt) {
    public static WalletDTO fromWallet(Wallet wallet) {
        return new WalletDTO(
                wallet.walletId(),
                wallet.address(),
                wallet.publicKey(),
                wallet.createdAt());
    }
}
