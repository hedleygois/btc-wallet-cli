package com.btcwallet.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import com.btcwallet.balance.WalletBalance;
import com.btcwallet.network.BitcoinBroadcastException;
import com.btcwallet.network.BitcoinNodeClient;
import com.btcwallet.network.FeeCalculator;
import com.btcwallet.network.NetworkMonitor;
import com.btcwallet.wallet.Wallet;
import com.btcwallet.wallet.WalletService;

/**
 * Service for creating and managing Bitcoin transactions.
 * Supports both simulation and real execution flows.
 */
public class TransactionService {

    private final WalletService walletService;
    private final FeeCalculator feeCalculator;
    private final NetworkMonitor networkMonitor;
    private final BitcoinNodeClient bitcoinNodeClient;

    /**
     * Creates a new TransactionService.
     *
     * @param walletService     Wallet service for accessing wallets
     * @param feeCalculator     Fee calculator for determining transaction fees
     * @param networkMonitor    Network monitor for checking network conditions
     * @param bitcoinNodeClient Bitcoin node client for broadcasting transactions
     */
    public TransactionService(WalletService walletService, FeeCalculator feeCalculator,
            NetworkMonitor networkMonitor, BitcoinNodeClient bitcoinNodeClient) {
        this.walletService = walletService;
        this.feeCalculator = feeCalculator;
        this.networkMonitor = networkMonitor;
        this.bitcoinNodeClient = bitcoinNodeClient;
    }

    /**
     * Creates a new transaction.
     * Defaults to simulation mode for safety.
     *
     * @param walletId         Wallet ID to use for the transaction
     * @param recipientAddress Bitcoin address of the recipient
     * @param amount           Amount to send in satoshis
     * @param isSimulation     Whether to simulate (true) or execute (false) the
     *                         transaction
     * @return Created transaction
     * @throws TransactionException If transaction creation fails
     */
    public Transaction createTransaction(String walletId, String recipientAddress, long amount, boolean isSimulation)
            throws TransactionException {
        try {
            if (amount <= 0) {
                throw TransactionException.invalidTransaction("Amount must be positive");
            }

            Wallet wallet = Optional.ofNullable(walletService.getWallet(walletId))
                    .orElseThrow(() -> TransactionException.invalidTransaction("Wallet not found: " + walletId));

            if (!walletService.isValidAddress(recipientAddress)) {
                throw TransactionException.invalidTransaction("Invalid recipient address: " + recipientAddress);
            }

            // Fetch UTXOs for coin selection
            var balance = walletService.getWalletBalance(walletId);
            List<WalletBalance.UTXO> utxos = balance.getUtxos();

            // Initial fee estimation (simplified for the unsigned structure)
            long estimatedFee = feeCalculator
                    .calculateFee(new org.bitcoinj.core.Transaction(wallet.networkParameters()));

            org.bitcoinj.core.Transaction unsignedTx = createUnsignedTransaction(wallet, recipientAddress, amount,
                    estimatedFee, utxos)
                    .orElseThrow(() -> TransactionException
                            .invalidTransaction("Insufficient funds or no spendable UTXOs for amount: " + amount));

            // Calculate final fee based on the actual transaction size
            long finalFee = feeCalculator.calculateFee(unsignedTx);

            // Sign transaction
            org.bitcoinj.core.Transaction signedTransaction = signTransaction(unsignedTx, wallet);

            // Create transaction record
            Transaction transaction = Transaction.fromTransaction(walletId, signedTransaction, isSimulation, finalFee);

            return isSimulation ? handleSimulation(transaction) : handleRealExecution(transaction);

        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            throw TransactionException.transactionFailed("Failed to create transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an unsigned Bitcoin transaction with UTXO selection and change
     * output.
     *
     * @param wallet           Wallet to send from
     * @param recipientAddress Recipient Bitcoin address
     * @param amount           Amount to send in satoshis
     * @param fee              Fee to include in satoshis
     * @param utxos            Available UTXOs
     * @return Optional containing the unsigned transaction
     */
    private Optional<org.bitcoinj.core.Transaction> createUnsignedTransaction(
            Wallet wallet, String recipientAddress, long amount, long fee, List<WalletBalance.UTXO> utxos) {

        NetworkParameters params = wallet.networkParameters();
        long targetAmount = amount + fee;

        // Coin Selection: Greedy approach to pick UTXOs (remember: UTXO are like cash bills!!!) until target is met
        List<WalletBalance.UTXO> selectedUtxos = new ArrayList<>();
        long totalInput = 0;
        for (WalletBalance.UTXO utxo : utxos) {
            selectedUtxos.add(utxo);
            totalInput += utxo.getValue().getValue();
            if (totalInput >= targetAmount)
                break;
        }

        if (totalInput < targetAmount) {
            return Optional.empty();
        }

        org.bitcoinj.core.Transaction transaction = new org.bitcoinj.core.Transaction(params);

        // Map selected UTXOs to TransactionInputs
        selectedUtxos.stream()
                .map(utxo -> {
                    TransactionOutPoint outPoint = new TransactionOutPoint(params, utxo.getOutputIndex(),
                            Sha256Hash.wrap(utxo.getTransactionHash()));
                    return new TransactionInput(params, transaction, new byte[] {}, outPoint, utxo.getValue());
                })
                .forEach(transaction::addInput);

        // Add recipient output
        transaction.addOutput(Coin.valueOf(amount), Address.fromString(params, recipientAddress));

        // Add change output if remainder is above dust limit
        long changeValue = totalInput - targetAmount;
        if (changeValue > org.bitcoinj.core.Transaction.MIN_NONDUST_OUTPUT.getValue()) {
            transaction.addOutput(Coin.valueOf(changeValue), Address.fromString(params, wallet.address()));
        }

        return Optional.of(transaction);
    }

    /**
     * Signs a transaction with the wallet's private key.
     *
     * @param unsignedTransaction Transaction to sign
     * @param wallet              Wallet containing the private key
     * @return Signed transaction
     */
    private org.bitcoinj.core.Transaction signTransaction(org.bitcoinj.core.Transaction unsignedTransaction,
            Wallet wallet) {
        try {
            ECKey ecKey = wallet.toECKey();
            NetworkParameters params = wallet.networkParameters();

            // For P2PKH (Legacy) addresses, we sign against the output script of the
            // address
            Script scriptPubKey = ScriptBuilder.createOutputScript(Address.fromString(params, wallet.address()));

            List<TransactionInput> inputs = unsignedTransaction.getInputs();
            for (int i = 0; i < inputs.size(); i++) {
                // SigHash.ALL (0x01) signs all inputs and outputs to ensure transaction
                // integrity
                TransactionSignature signature = unsignedTransaction.calculateSignature(
                        i, ecKey, scriptPubKey, org.bitcoinj.core.Transaction.SigHash.ALL, false);
                inputs.get(i).setScriptSig(ScriptBuilder.createInputScript(signature, ecKey));
            }

            return unsignedTransaction;
        } catch (Exception e) {
            throw TransactionException.signingFailed("Cryptographic signing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Handles simulation flow (default).
     * Transaction is created and signed but not broadcasted.
     *
     * @param transaction Transaction to simulate
     * @return Transaction with SIMULATED status
     */
    private Transaction handleSimulation(Transaction transaction) {
        // Validate the signed transaction without broadcasting
        validateTransaction(transaction.rawTransaction());

        // Return transaction with SIMULATED status
        return new Transaction(
                transaction.transactionId(),
                transaction.walletId(),
                transaction.recipientAddress(),
                transaction.amount(),
                transaction.fee(),
                Transaction.TransactionStatus.SIMULATED,
                transaction.createdAt(),
                true,
                transaction.rawTransaction());
    }

    /**
     * Handles real execution flow.
     * Transaction is broadcasted to the Bitcoin network.
     *
     * @param transaction Transaction to execute
     * @return Transaction with BROADCASTED status
     * @throws TransactionException If transaction execution fails
     */
    private Transaction handleRealExecution(Transaction transaction) throws TransactionException {
        try {
            // Validate transaction
            validateTransaction(transaction.rawTransaction());

            // Broadcast transaction to network
            broadcastTransaction(transaction.rawTransaction());

            // Return transaction with BROADCASTED status
            return new Transaction(
                    transaction.transactionId(),
                    transaction.walletId(),
                    transaction.recipientAddress(),
                    transaction.amount(),
                    transaction.fee(),
                    Transaction.TransactionStatus.BROADCASTED,
                    transaction.createdAt(),
                    false,
                    transaction.rawTransaction());

        } catch (BitcoinBroadcastException e) {
            // Wrap our checked exception in the existing TransactionException
            throw TransactionException.networkError(e.getMessage(), e);
        } catch (Exception e) {
            throw TransactionException.transactionFailed("Failed to broadcast transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Validates a transaction before execution.
     *
     * @param transaction Transaction to validate
     * @throws TransactionException If validation fails
     */
    private void validateTransaction(org.bitcoinj.core.Transaction transaction) {
        // Basic validation checks
        if (transaction.getInputs().isEmpty()) {
            throw TransactionException.invalidTransaction("Transaction has no inputs");
        }

        if (transaction.getOutputs().isEmpty()) {
            throw TransactionException.invalidTransaction("Transaction has no outputs");
        }

        long totalOutput = transaction.getOutputs().stream()
                .mapToLong(output -> output.getValue().getValue())
                .sum();

        if (totalOutput <= 0) {
            throw TransactionException.invalidTransaction("Transaction has no value");
        }
    }

    /**
     * Broadcasts a transaction to the Bitcoin network.
     *
     * @param transaction Transaction to broadcast
     * @throws BitcoinBroadcastException If broadcasting fails
     * @throws IllegalArgumentException  If transaction is invalid
     */
    private void broadcastTransaction(org.bitcoinj.core.Transaction transaction)
            throws BitcoinBroadcastException {

        // Validate input
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        try {
            // Check if Bitcoin node connection is enabled
            if (bitcoinNodeClient.getConfig().isEnabled()) {
                // Use real Bitcoin node connection
                bitcoinNodeClient.broadcastTransaction(transaction);
            } else {
                // Fallback to simulation mode when node connection is disabled
                System.out.println("📡 [SIMULATION] Broadcasting transaction: " +
                        transaction.getTxId());

                // Still check network conditions for simulation
                if (!networkMonitor.isNetworkAvailable()) {
                    throw BitcoinBroadcastException.networkUnavailable();
                }

                if (networkMonitor.getMempoolSize() > 10000) {
                    throw BitcoinBroadcastException.networkCongestion();
                }
            }

        } catch (BitcoinBroadcastException e) {
            // Re-throw our checked exception
            throw e;
        } catch (Exception e) {
            // Wrap any unexpected exceptions in our checked exception
            throw BitcoinBroadcastException.broadcastFailed(e.getMessage());
        }
    }

    /**
     * Gets transaction status.
     *
     * @param transactionId Transaction ID to check
     * @return Current transaction status
     */
    public Transaction.TransactionStatus getTransactionStatus(String transactionId) {
        // In a real implementation, this would check the blockchain
        // For simulation purposes, we'll return a reasonable status
        return Transaction.TransactionStatus.CONFIRMING;
    }

    /**
     * Lists all transactions for a wallet.
     *
     * @param walletId Wallet ID
     * @return List of transactions
     */
    public List<Transaction> listTransactions(String walletId) {
        // In a real implementation, this would query transaction storage
        // For now, return empty list
        return List.of();
    }
}