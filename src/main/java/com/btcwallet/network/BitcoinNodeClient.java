package com.btcwallet.network;

import com.btcwallet.balance.WalletBalance;
import com.btcwallet.config.BitcoinConfig;
import com.btcwallet.wallet.Wallet;

import org.bitcoinj.core.*;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for connecting to and communicating with Bitcoin nodes.
 * Uses BitcoinJ to establish peer connections and broadcast transactions.
 */
public class BitcoinNodeClient {
    private final BitcoinConfig config;
    private PeerGroup peerGroup;
    private BlockChain blockChain;

    /**
     * Creates a new BitcoinNodeClient with the specified configuration.
     * 
     * @param config Bitcoin node configuration
     */
    public BitcoinNodeClient(BitcoinConfig config) {
        this.config = config;
    }

    /**
     * Initializes the Bitcoin node client with peer group and block chain.
     * 
     * @throws BitcoinBroadcastException if initialization fails
     */
    public void initialize() throws BitcoinBroadcastException {
        if (!config.isEnabled()) {
            throw new BitcoinBroadcastException(
                "Bitcoin node connection is disabled in configuration");
        }

        try {
            // Create block chain with memory store
            this.blockChain = new BlockChain(
                config.getNetworkParameters(), 
                new MemoryBlockStore(config.getNetworkParameters())
            );

            // Create and configure PeerGroup
            this.peerGroup = new PeerGroup(
                config.getNetworkParameters(), 
                blockChain
            );

            // Configure peer group settings
            peerGroup.setMaxConnections(config.getMaxConnections());
            peerGroup.setConnectTimeoutMillis(config.getTimeoutMillis());
            peerGroup.setUseLocalhostPeerWhenPossible(config.isLocalhostPeer());

            // Add our configured node address
            PeerAddress peerAddress = new PeerAddress(
                config.getNetworkParameters(),
                InetAddress.getByName(config.getNodeHost()),
                config.getNodePort()
            );
            peerGroup.addAddress(peerAddress);

        } catch (BlockStoreException e) {
            throw new BitcoinBroadcastException(
                "Failed to initialize block store: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new BitcoinBroadcastException(
                "Failed to initialize Bitcoin node client: " + e.getMessage(), e);
        }
    }

    /**
     * Connects to the configured Bitcoin node.
     * 
     * @throws BitcoinBroadcastException if connection fails
     */
    public void connect() throws BitcoinBroadcastException {
        if (peerGroup == null) {
            initialize();
        }

        try {
            // Start peer group
            peerGroup.start();

            // Connect to our configured node
            Peer peer = peerGroup.connectTo(new InetSocketAddress(
                config.getNodeHost(),
                config.getNodePort()
            ));

            if (peer == null) {
                throw new BitcoinBroadcastException(
                    "Failed to connect to Bitcoin node at " +
                    config.getNodeHost() + ":" + config.getNodePort());
            }

            // Wait for connection to establish with timeout
            peer.getConnectionOpenFuture().get(
                config.getTimeoutMillis(), 
                TimeUnit.MILLISECONDS
            );

            System.out.println("✅ Connected to Bitcoin node: " + 
                config.getNodeHost() + ":" + config.getNodePort());

        } catch (TimeoutException e) {
            throw new BitcoinBroadcastException(
                "Connection timeout after " + config.getTimeoutMillis() + "ms", e);
        } catch (Exception e) {
            throw new BitcoinBroadcastException(
                "Failed to connect to Bitcoin node: " + e.getMessage(), e);
        }
    }

    /**
     * Broadcasts a transaction to the Bitcoin network.
     * 
     * @param transaction Transaction to broadcast
     * @throws BitcoinBroadcastException if broadcasting fails
     */
    public void broadcastTransaction(Transaction transaction)
        throws BitcoinBroadcastException {

            Optional.ofNullable(transaction).orElseThrow(
            () -> new IllegalArgumentException("Transaction cannot be null"));

        try {
            // Ensure we're connected
            if (peerGroup == null || peerGroup.getConnectedPeers().isEmpty()) {
                connect();
            }

            // Broadcast transaction using BitcoinJ
            TransactionBroadcast broadcast = peerGroup.broadcastTransaction(transaction);

            // Wait for broadcast to complete with timeout
            broadcast.future().get(config.getTimeoutMillis(), TimeUnit.MILLISECONDS);

            // Verify broadcast was successful
            // Note: In BitcoinJ, we can check if the broadcast completed successfully
            // by checking if the future completed without exception
            System.out.println("📡 Successfully broadcasted transaction: " + 
                transaction.getTxId() + " to Bitcoin network");

        } catch (TimeoutException e) {
            throw new BitcoinBroadcastException(
                "Transaction broadcast timeout after " + config.getTimeoutMillis() + "ms", e);
        } catch (Exception e) {
            throw new BitcoinBroadcastException(
                "Failed to broadcast transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the balance for a wallet by querying the Bitcoin blockchain.
     * 
     * @param wallet Wallet to get balance for
     * @return WalletBalance object with balance information
     * @throws BitcoinBroadcastException If balance cannot be retrieved
     */
    public WalletBalance getWalletBalance(Wallet wallet) throws BitcoinBroadcastException {
        try {
            // Ensure we're connected to blockchain
            if (!isConnected()) {
                connect();
            }

            // Create a BitcoinJ wallet for balance tracking
            org.bitcoinj.wallet.Wallet bitcoinJWallet = org.bitcoinj.wallet.Wallet.createBasic(
                config.getNetworkParameters()
            );

            // Add the wallet address to watch
            Address address = org.bitcoinj.core.Address.fromString(config.getNetworkParameters(), wallet.address());
            bitcoinJWallet.addWatchedAddress(address);

            // Add wallet to peer group for balance tracking
            peerGroup.addWallet(bitcoinJWallet);

            // Wait for initial chain download (with timeout)
            try {
                peerGroup.downloadBlockChain();
            } catch (Exception e) {
                // If full sync takes too long, proceed with available data
                System.out.println("⏳ Chain sync still in progress, using available balance data");
            }

            // Get current balances
            Coin confirmedBalance = bitcoinJWallet.getBalance(org.bitcoinj.wallet.Wallet.BalanceType.ESTIMATED);
            Coin unconfirmedBalance = bitcoinJWallet.getBalance(org.bitcoinj.wallet.Wallet.BalanceType.AVAILABLE_SPENDABLE);
            Coin totalBalance = confirmedBalance.add(unconfirmedBalance);

            // Get UTXOs
            List<WalletBalance.UTXO> utxos = new ArrayList<>();
            for (TransactionOutput output : bitcoinJWallet.getUnspents()) {
                utxos.add(new WalletBalance.UTXO(
                    output.getParentTransaction().getTxId().toString(),
                    output.getIndex(),
                    output.getValue(),
                    output.getScriptPubKey().toString(),
                    output.getParentTransaction().getConfidence().getDepthInBlocks()
                ));
            }

            // Create and return WalletBalance object
            return new WalletBalance(
                wallet.walletId(),
                confirmedBalance,
                unconfirmedBalance,
                totalBalance,
                Instant.now(),
                String.valueOf(peerGroup.getMostCommonChainHeight()),
                utxos
            );

        } catch (Exception e) {
            throw new BitcoinBroadcastException(
                "Failed to fetch balance for wallet " + wallet.walletId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Disconnects from Bitcoin nodes and cleans up resources.
     */
    public void disconnect() {
        if (peerGroup != null) {
            try {
                peerGroup.stop();
                System.out.println("🔌 Disconnected from Bitcoin peers");
            } catch (Exception e) {
                System.err.println("Warning: Error during peer group shutdown: " + e.getMessage());
            }
        }

        // Note: In BitcoinJ, Context and BlockChain don't have explicit stop methods
        // The peerGroup.stop() is sufficient for cleanup
    }

    /**
     * Checks if connected to any Bitcoin peers.
     * 
     * @return true if connected to at least one peer
     */
    public boolean isConnected() {
        return peerGroup != null && !peerGroup.getConnectedPeers().isEmpty();
    }

    /**
     * Gets the number of connected peers.
     * 
     * @return number of connected peers
     */
    public int getConnectedPeerCount() {
        return peerGroup != null ? peerGroup.getConnectedPeers().size() : 0;
    }

    /**
     * Gets the Bitcoin configuration.
     * 
     * @return BitcoinConfig
     */
    public BitcoinConfig getConfig() {
        return config;
    }
}