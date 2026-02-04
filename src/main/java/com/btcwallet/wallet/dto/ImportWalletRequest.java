package com.btcwallet.wallet.dto;

public class ImportWalletRequest {
    private String key; // Can be private key (hex or WIF) or mnemonic

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
