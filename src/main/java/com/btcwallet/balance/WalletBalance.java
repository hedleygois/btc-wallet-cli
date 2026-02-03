package com.btcwallet.balance;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.bitcoinj.core.Coin;

/**
 * Represents the balance information for a Bitcoin wallet.
 * Includes confirmed, unconfirmed, and total balances along with metadata.
 */
public class WalletBalance {
    private final String walletId;
    private final Coin confirmedBalance;
    private final Coin unconfirmedBalance;
    private final Coin totalBalance;
    private final Instant lastUpdated;
    private final String blockchainHeight;
    private final List<UTXO> utxos;

    /**
     * Creates a new WalletBalance.
     *
     * @param walletId Wallet ID
     * @param confirmedBalance Confirmed balance (in satoshis)
     * @param unconfirmedBalance Unconfirmed balance (in satoshis)
     * @param totalBalance Total balance (in satoshis)
     * @param lastUpdated When the balance was last updated
     * @param blockchainHeight Current blockchain height
     * @param utxos List of unspent transaction outputs
     */
    public WalletBalance(String walletId, Coin confirmedBalance, Coin unconfirmedBalance, 
                        Coin totalBalance, Instant lastUpdated, String blockchainHeight, List<UTXO> utxos) {
        this.walletId = walletId;
        this.confirmedBalance = confirmedBalance;
        this.unconfirmedBalance = unconfirmedBalance;
        this.totalBalance = totalBalance;
        this.lastUpdated = lastUpdated;
        this.blockchainHeight = blockchainHeight;
        this.utxos = List.copyOf(utxos);
    }

    /**
     * Gets the wallet ID.
     *
     * @return Wallet ID
     */
    public String getWalletId() {
        return walletId;
    }

    /**
     * Gets the confirmed balance in satoshis.
     *
     * @return Confirmed balance
     */
    public Coin getConfirmedBalance() {
        return confirmedBalance;
    }

    /**
     * Gets the confirmed balance in BTC as a string.
     *
     * @return Confirmed balance in BTC
     */
    public String getConfirmedBalanceInBTC() {
        return confirmedBalance.toFriendlyString();
    }

    /**
     * Gets the unconfirmed balance in satoshis.
     *
     * @return Unconfirmed balance
     */
    public Coin getUnconfirmedBalance() {
        return unconfirmedBalance;
    }

    /**
     * Gets the unconfirmed balance in BTC as a string.
     *
     * @return Unconfirmed balance in BTC
     */
    public String getUnconfirmedBalanceInBTC() {
        return unconfirmedBalance.toFriendlyString();
    }

    /**
     * Gets the total balance in satoshis.
     *
     * @return Total balance
     */
    public Coin getTotalBalance() {
        return totalBalance;
    }

    /**
     * Gets the total balance in BTC as a string.
     *
     * @return Total balance in BTC
     */
    public String getBalanceInBTC() {
        return totalBalance.toFriendlyString();
    }

    /**
     * Gets when the balance was last updated.
     *
     * @return Last updated timestamp
     */
    public Instant getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Gets the current blockchain height.
     *
     * @return Blockchain height
     */
    public String getBlockchainHeight() {
        return blockchainHeight;
    }

    /**
     * Gets the list of unspent transaction outputs.
     *
     * @return List of UTXOs
     */
    public List<UTXO> getUtxos() {
        return utxos;
    }

    /**
     * Checks if the wallet has sufficient funds for a transaction.
     *
     * @param amount Amount required (in satoshis)
     * @return true if sufficient funds, false otherwise
     */
    public boolean hasSufficientFunds(Coin amount) {
        return totalBalance.compareTo(amount) >= 0;
    }

    /**
     * Checks if the wallet has sufficient confirmed funds for a transaction.
     *
     * @param amount Amount required (in satoshis)
     * @return true if sufficient confirmed funds, false otherwise
     */
    public boolean hasSufficientConfirmedFunds(Coin amount) {
        return confirmedBalance.compareTo(amount) >= 0;
    }

    /**
     * Gets the total number of UTXOs.
     *
     * @return Number of UTXOs
     */
    public int getUtxoCount() {
        return utxos.size();
    }

    /**
     * Gets the total value of all UTXOs.
     *
     * @return Total UTXO value
     */
    public Coin getUtxoTotalValue() {
        return utxos.stream()
                .map(UTXO::getValue)
                .reduce(Coin.ZERO, Coin::add);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalletBalance that = (WalletBalance) o;
        return walletId.equals(that.walletId) &&
                confirmedBalance.equals(that.confirmedBalance) &&
                unconfirmedBalance.equals(that.unconfirmedBalance) &&
                totalBalance.equals(that.totalBalance) &&
                lastUpdated.equals(that.lastUpdated) &&
                blockchainHeight.equals(that.blockchainHeight) &&
                utxos.equals(that.utxos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(walletId, confirmedBalance, unconfirmedBalance, 
                          totalBalance, lastUpdated, blockchainHeight, utxos);
    }

    @Override
    public String toString() {
        return "WalletBalance{" +
                "walletId='" + walletId + '\'' +
                ", confirmedBalance=" + confirmedBalance +
                ", unconfirmedBalance=" + unconfirmedBalance +
                ", totalBalance=" + totalBalance +
                ", lastUpdated=" + lastUpdated +
                ", blockchainHeight='" + blockchainHeight + '\'' +
                ", utxos=" + utxos +
                '}';
    }

    /**
     * Represents an Unspent Transaction Output (UTXO).
     */
    public static class UTXO {
        private final String transactionHash;
        private final int outputIndex;
        private final Coin value;
        private final String scriptPubKey;
        private final int confirmations;

        public UTXO(String transactionHash, int outputIndex, Coin value, 
                   String scriptPubKey, int confirmations) {
            this.transactionHash = transactionHash;
            this.outputIndex = outputIndex;
            this.value = value;
            this.scriptPubKey = scriptPubKey;
            this.confirmations = confirmations;
        }

        public String getTransactionHash() { return transactionHash; }
        public int getOutputIndex() { return outputIndex; }
        public Coin getValue() { return value; }
        public String getScriptPubKey() { return scriptPubKey; }
        public int getConfirmations() { return confirmations; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UTXO utxo = (UTXO) o;
            return outputIndex == utxo.outputIndex &&
                    confirmations == utxo.confirmations &&
                    transactionHash.equals(utxo.transactionHash) &&
                    value.equals(utxo.value) &&
                    scriptPubKey.equals(utxo.scriptPubKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(transactionHash, outputIndex, value, scriptPubKey, confirmations);
        }

        @Override
        public String toString() {
            return "UTXO{" +
                    "txHash='" + transactionHash + '\'' +
                    ", index=" + outputIndex +
                    ", value=" + value +
                    ", confirmations=" + confirmations +
                    '}';
        }
    }
}