package com.btcwallet.transaction;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.btcwallet.network.FeeCalculator;
import com.btcwallet.network.NetworkMonitor;
import com.btcwallet.transaction.dto.CreateTransactionRequest;
import com.btcwallet.transaction.dto.TransactionDTO;

@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    private final TransactionService transactionService;
    private final FeeCalculator feeCalculator;
    private final NetworkMonitor networkMonitor;

    public TransactionController(TransactionService transactionService, FeeCalculator feeCalculator, NetworkMonitor networkMonitor) {
        this.transactionService = transactionService;
        this.feeCalculator = feeCalculator;
        this.networkMonitor = networkMonitor;
    }

    /**
     * Creates a new Bitcoin transaction.
     *
     * @param request The request body containing wallet ID, recipient address, amount, and simulation flag.
     * @return A TransactionDTO representing the created transaction.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createTransaction(@RequestBody CreateTransactionRequest request) {
        try {
            long amountSatoshis = FeeCalculator.btcToSatoshis(request.getAmountBtc().doubleValue());
            
            Transaction transaction = transactionService.createTransaction(
                request.getWalletId(),
                request.getRecipientAddress(),
                amountSatoshis,
                request.isSimulate()
            );
            return new ResponseEntity<>(TransactionDTO.fromTransaction(transaction), HttpStatus.CREATED);
        } catch (TransactionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Estimates transaction fees for a typical transaction.
     *
     * @return A map containing fee estimates for low, medium, and high priority, and recommended priority.
     */
    @GetMapping("/fee-estimate")
    public ResponseEntity<FeeEstimateResponse> getFeeEstimate() {
        int typicalTransactionSize = 226; // As used in CLI
        FeeCalculator.FeeEstimate estimates = feeCalculator.getFeeEstimates(typicalTransactionSize);

        FeeEstimateResponse response = new FeeEstimateResponse(
            estimates.low(),
            FeeCalculator.satoshisToBTC(estimates.low()),
            estimates.medium(),
            FeeCalculator.satoshisToBTC(estimates.medium()),
            estimates.high(),
            FeeCalculator.satoshisToBTC(estimates.high()),
            networkMonitor.getFeeRecommendation().name(),
            networkMonitor.getNetworkStatus()
        );
        return ResponseEntity.ok(response);
    }
}
