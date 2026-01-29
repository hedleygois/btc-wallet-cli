package com.btcwallet.config;

import com.btcwallet.exception.BitcoinConfigurationException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.core.NetworkParameters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class for Bitcoin node connections.
 * Loads settings from bitcoin.properties file.
 */
public class BitcoinConfig {
    private final String nodeHost;
    private final int nodePort;
    private final boolean testnet;
    private final int timeoutMillis;
    private final int maxConnections;
    private final boolean enabled;
    private final boolean localhostPeer;

    /**
     * Creates a new BitcoinConfig by loading from bitcoin.properties.
     * 
     * @throws BitcoinConfigurationException if configuration cannot be loaded or is invalid
     */
    public BitcoinConfig() throws BitcoinConfigurationException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("bitcoin.properties")) {

            if (input == null) {
                throw new BitcoinConfigurationException(
                    "bitcoin.properties not found in classpath");
            }

            props.load(input);

            // Load configuration with validation
            this.enabled = Boolean.parseBoolean(
                props.getProperty("bitcoin.node.enabled", "false"));
            this.nodeHost = props.getProperty("bitcoin.node.host", "localhost");
            this.nodePort = Integer.parseInt(
                props.getProperty("bitcoin.node.port", "8333"));
            this.testnet = Boolean.parseBoolean(
                props.getProperty("bitcoin.node.testnet", "false"));
            this.timeoutMillis = Integer.parseInt(
                props.getProperty("bitcoin.node.timeout", "30000"));
            this.maxConnections = Integer.parseInt(
                props.getProperty("bitcoin.node.max_connections", "3"));
            this.localhostPeer = Boolean.parseBoolean(
                props.getProperty("bitcoin.node.localhost_peer", "true"));

        } catch (IOException e) {
            throw new BitcoinConfigurationException(
                "Failed to load bitcoin.properties", e);
        } catch (NumberFormatException e) {
            throw new BitcoinConfigurationException(
                "Invalid number format in bitcoin.properties", e);
        }
    }

    /**
     * Gets the Bitcoin node host.
     * 
     * @return node host
     */
    public String getNodeHost() { 
        return nodeHost; 
    }

    /**
     * Gets the Bitcoin node port.
     * 
     * @return node port
     */
    public int getNodePort() { 
        return nodePort; 
    }

    /**
     * Checks if testnet should be used.
     * 
     * @return true if testnet, false if mainnet
     */
    public boolean isTestnet() { 
        return testnet; 
    }

    /**
     * Gets the connection timeout in milliseconds.
     * 
     * @return timeout in milliseconds
     */
    public int getTimeoutMillis() { 
        return timeoutMillis; 
    }

    /**
     * Gets the maximum number of peer connections.
     * 
     * @return maximum connections
     */
    public int getMaxConnections() { 
        return maxConnections; 
    }

    /**
     * Checks if Bitcoin node connection is enabled.
     * 
     * @return true if enabled, false if disabled
     */
    public boolean isEnabled() { 
        return enabled; 
    }

    /**
     * Checks if localhost peer detection is enabled.
     * 
     * @return true if localhost peer detection is enabled
     */
    public boolean isLocalhostPeer() { 
        return localhostPeer; 
    }

    /**
     * Gets the appropriate NetworkParameters based on testnet setting.
     * 
     * @return NetworkParameters for the configured network
     */
    public NetworkParameters getNetworkParameters() { 
        return testnet ? TestNet3Params.get() : MainNetParams.get(); 
    }

    @Override
    public String toString() {
        return "BitcoinConfig{" +
                "nodeHost='" + nodeHost + '\'' +
                ", nodePort=" + nodePort +
                ", testnet=" + testnet +
                ", timeoutMillis=" + timeoutMillis +
                ", maxConnections=" + maxConnections +
                ", enabled=" + enabled +
                ", localhostPeer=" + localhostPeer +
                '}';
    }
}