package com.btcwallet.network;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/network")
public class NetworkController {

    private final NetworkMonitor networkMonitor;

    public NetworkController(NetworkMonitor networkMonitor) {
        this.networkMonitor = networkMonitor;
    }

    /**
     * Retrieves current network information.
     *
     * @return A map containing network status.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getNetworkInfo() {
        Map<String, String> response = Map.of(
            "networkStatus", networkMonitor.getNetworkStatus()
        );
        return ResponseEntity.ok(response);
    }
}
