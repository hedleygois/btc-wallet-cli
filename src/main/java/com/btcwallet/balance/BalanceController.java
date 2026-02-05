package com.btcwallet.balance;

import com.btcwallet.wallet.WalletService;
import com.btcwallet.wallet.WalletException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/balance")
public class BalanceController {

    private final WalletService walletService;

    public BalanceController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Retrieves the balance for a given wallet ID.
     *
     * @param walletId The ID of the wallet.
     * @return A WalletBalance object.
     */
    @GetMapping("/{walletId}")
    public ResponseEntity<?> getWalletBalance(@PathVariable String walletId) {
        try {
            WalletBalance balance = walletService.getWalletBalance(walletId);
            return ResponseEntity.ok(balance);
        } catch (WalletException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
