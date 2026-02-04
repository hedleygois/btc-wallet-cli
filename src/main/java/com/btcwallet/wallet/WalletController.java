package com.btcwallet.wallet;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.btcwallet.wallet.dto.ImportWalletRequest;
import com.btcwallet.wallet.dto.WalletDTO;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Generates a new random Bitcoin wallet.
     * Does not expose the private key directly in the response.
     *
     * @return A WalletDTO containing public wallet information.
     */
    @PostMapping("/generate")
    public ResponseEntity<WalletDTO> generateWallet() {
        Wallet wallet = walletService.generateWallet();
        String networkName = walletService.getNetworkName(); // Assuming this is available
        return new ResponseEntity<>(WalletDTO.fromWallet(wallet, networkName), HttpStatus.CREATED);
    }

    /**
     * Generates a new Bitcoin wallet with a mnemonic seed phrase.
     * NOTE: The mnemonic phrase itself is highly sensitive and is NOT exposed via this API endpoint.
     * TODO: It should be displayed securely by a client application only once upon generation.
     *
     * @return A WalletDTO containing public wallet information.
     */
    @PostMapping("/generate-mnemonic")
    public ResponseEntity<WalletDTO> generateWalletWithMnemonic() {
        WalletGenerator.WalletGenerationResult result = walletService.generateWalletWithMnemonic();
        Wallet wallet = result.getWallet();
        String networkName = walletService.getNetworkName();
        // Do NOT expose result.getMnemonic() via the API
        return new ResponseEntity<>(WalletDTO.fromWallet(wallet, networkName), HttpStatus.CREATED);
    }

    /**
     * Imports a wallet from a private key (hex or WIF) or a mnemonic seed phrase.
     *
     * @param request Contains the key or mnemonic to import.
     * @return A WalletDTO containing public wallet information.
     * @throws WalletException if import fails.
     */
    @PostMapping("/import")
    public ResponseEntity<WalletDTO> importWallet(@RequestBody ImportWalletRequest request) {
        String input = request.getKey();
        Wallet wallet;
        String networkName = walletService.getNetworkName();

        try {
            // Heuristic to detect input type, similar to CLI
            if (walletService.isValidHexPrivateKey(input)) {
                wallet = walletService.importFromPrivateKey(input);
            } else if (walletService.isValidWIF(input)) {
                wallet = walletService.importFromWIF(input);
            } else if (walletService.isValidMnemonic(input)) {
                wallet = walletService.importFromMnemonic(input);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // Or a more specific error DTO
            }
            return ResponseEntity.ok(WalletDTO.fromWallet(wallet, networkName));
        } catch (WalletException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // Placeholder, improve with ErrorDTO
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validates if a given string is a valid Bitcoin address for the configured network.
     *
     * @param address The Bitcoin address to validate.
     * @return True if the address is valid, false otherwise.
     */
    @GetMapping("/validate/{address}")
    public ResponseEntity<Boolean> validateAddress(@PathVariable String address) {
        boolean isValid = walletService.isValidAddress(address);
        return ResponseEntity.ok(isValid);
    }
}
