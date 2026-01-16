# BTC Wallet Management App Architecture

## Overview
A simple Bitcoin wallet management application using BitcoinJ library. The application provides basic wallet functionality including wallet generation, display of public addresses, import of existing wallets, and transaction creation with simulation and real execution capabilities.

## System Architecture

### High-Level Components

```
┌───────────────────────────────────────────────────────┐
│                 BTC Wallet Management App               │
├───────────────────────────────────────────────────────┤
│                                                       │
│  ┌─────────────┐    ┌─────────────┐    ┌───────────┐  │
│  │   Wallet    │    │   Wallet    │    │  BitcoinJ │  │
│  │  Generator  │    │   Importer  │    │  Library  │  │
│  └─────────────┘    └─────────────┘    └───────────┘  │
│          │                  │                     │     │
│          ▼                  ▼                     │     │
│  ┌───────────────────────────────────────────────┐  │
│  │               Wallet Service                 │  │
│  └───────────────────────────────────────────────┘  │
│                  │                              │     │
│                  ▼                              ▼     │
│  ┌─────────────────────┐    ┌───────────────────┐  │
│  │   Wallet Storage   │    │   User Interface  │  │
│  └─────────────────────┘    └───────────────────┘  │
│                                                       │
│  ┌───────────────────────────────────────────────┐  │
│  │          Transaction Service                 │  │
│  └───────────────────────────────────────────────┘  │
│                  │                              │     │
│                  ▼                              ▼     │
│  ┌─────────────────────┐    ┌───────────────────┐  │
│  │   Fee Calculator   │    │   Network Monitor │  │
│  └─────────────────────┘    └───────────────────┘  │
│                                                       │
└───────────────────────────────────────────────────────┘
```

### Component Details

#### 1. Wallet Generator
- **Responsibility**: Creates new Bitcoin wallets with public/private key pairs
- **Dependencies**: BitcoinJ library
- **Output**: Wallet object containing key pair and address

#### 2. Wallet Importer
- **Responsibility**: Imports existing wallets from private keys or seed phrases
- **Dependencies**: BitcoinJ library
- **Input**: Private key or seed phrase
- **Output**: Wallet object

#### 3. Wallet Service
- **Responsibility**: Core business logic for wallet operations
- **Functions**:
  - Generate new wallet
  - Import existing wallet
  - Get wallet address
  - Validate wallet data
- **Dependencies**: Wallet Generator, Wallet Importer

#### 4. Transaction Service
- **Responsibility**: Core business logic for transaction operations
- **Functions**:
  - Create new transactions (simulation and real)
  - Sign transactions with wallet private keys
  - Calculate transaction fees based on network conditions
  - Validate transaction data
- **Dependencies**: Wallet Service, Fee Calculator, Network Monitor

#### 5. Fee Calculator
- **Responsibility**: Calculate appropriate transaction fees
- **Functions**:
  - Monitor network conditions
  - Calculate fee based on transaction size and network congestion
  - Provide fee estimates for different priority levels
- **Dependencies**: Network Monitor

#### 6. Network Monitor
- **Responsibility**: Monitor Bitcoin network conditions
- **Functions**:
  - Track current network congestion
  - Monitor mempool size
  - Provide real-time network statistics
- **Dependencies**: BitcoinJ library

#### 4. Wallet Storage
- **Responsibility**: Persistent storage of wallet information
- **Implementation**: Simple file-based storage (JSON format)
- **Data**: Wallet address, public key, encrypted private key

#### 7. User Interface
- **Responsibility**: Command-line interface for user interaction
- **Functions**:
  - Display menu options
  - Handle user input
  - Display wallet information
  - Show error messages
  - Transaction creation interface
  - Transaction simulation vs real execution selection

### Data Flow

#### Wallet Generation Flow
1. User requests new wallet via UI
2. UI calls Wallet Service.generateWallet()
3. Wallet Service delegates to Wallet Generator
4. Wallet Generator uses BitcoinJ to create key pair
5. Wallet object returned to Wallet Service
6. Wallet Service stores wallet via Wallet Storage
7. UI displays wallet address to user

#### Wallet Import Flow
1. User provides private key or seed phrase via UI
2. UI calls Wallet Service.importWallet(input)
3. Wallet Service delegates to Wallet Importer
4. Wallet Importer uses BitcoinJ to reconstruct wallet
5. Wallet object returned to Wallet Service
6. Wallet Service validates and stores wallet
7. UI displays imported wallet address

#### Transaction Creation Flow (Simulation - Default)
1. User selects "Create Transaction" via UI
2. UI prompts for recipient address and amount
3. UI asks user to choose between simulation (default) and real transaction
4. User selects simulation mode
5. UI calls Transaction Service.createTransaction(walletId, recipient, amount, isSimulation=true)
6. Transaction Service retrieves wallet from Wallet Service
7. Transaction Service calculates fee using Fee Calculator
8. Transaction Service creates unsigned transaction
9. Transaction Service signs transaction with wallet private key
10. Transaction Service validates transaction
11. Transaction Service returns signed transaction (not broadcasted)
12. UI displays transaction details and simulation results

#### Transaction Creation Flow (Real Execution)
1. User selects "Create Transaction" via UI
2. UI prompts for recipient address and amount
3. UI asks user to choose between simulation (default) and real transaction
4. User selects real execution mode
5. UI calls Transaction Service.createTransaction(walletId, recipient, amount, isSimulation=false)
6. Transaction Service retrieves wallet from Wallet Service
7. Transaction Service calculates fee using Fee Calculator
8. Transaction Service creates unsigned transaction
9. Transaction Service signs transaction with wallet private key
10. Transaction Service validates transaction
11. Transaction Service broadcasts transaction to Bitcoin network
12. Transaction Service monitors confirmation status
13. UI displays transaction details and confirmation progress

### Technology Stack

- **Language**: Java 21 (LTS)
- **Build System**: Gradle
- **Bitcoin Library**: BitcoinJ 0.16.3
- **Testing**: JUnit 5.10.0
- **Storage**: JSON files

### Class Structure

```
com.btcwallet
├── Main.java                          # Entry point
├── service
│   ├── WalletService.java             # Core wallet operations
│   ├── WalletGenerator.java           # Wallet creation
│   ├── WalletImporter.java            # Wallet import
│   ├── TransactionService.java        # Transaction operations
│   ├── FeeCalculator.java             # Fee calculation
│   └── NetworkMonitor.java            # Network monitoring
├── model
│   ├── Wallet.java                    # Wallet data model
│   └── Transaction.java               # Transaction data model
├── storage
│   ├── WalletStorage.java             # Wallet persistence
│   └── TransactionStorage.java        # Transaction persistence
├── cli
│   └── WalletCLI.java                 # Command line interface
└── exception
    ├── WalletException.java           # Wallet exceptions
    └── TransactionException.java       # Transaction exceptions
```

### Key Design Principles

1. **Separation of Concerns**: Clear separation between UI, business logic, and data layers
2. **Single Responsibility**: Each class has a single, well-defined purpose
3. **Dependency Injection**: Components depend on interfaces, not concrete implementations
4. **Immutability**: Wallet objects are immutable once created
5. **Error Handling**: Comprehensive exception handling with meaningful error messages

### Security Considerations

1. **Private Key Protection**: Private keys should never be logged or displayed
2. **Input Validation**: All user inputs must be validated
3. **Error Handling**: Sensitive information should not leak through error messages
4. **Storage Security**: Private keys should be encrypted when stored
5. **Transaction Security**: Transactions should be properly signed and validated
6. **Network Security**: All network communications should be encrypted

### Testing Strategy

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **Mocking**: Use mock objects for external dependencies (BitcoinJ)
4. **Test Coverage**: Aim for 80%+ code coverage
5. **Transaction Testing**: Test both simulation and real transaction flows
6. **Fee Calculation Testing**: Test fee calculation under different network conditions

### Future Extensions

1. **Multi-Wallet Support**: Manage multiple wallets
2. **Balance Checking**: Query blockchain for wallet balance
3. **QR Code Support**: Generate QR codes for addresses
4. **Hardware Wallet Integration**: Support for hardware wallets
5. **Transaction History**: View past transactions
6. **Advanced Fee Management**: Custom fee strategies

## Implementation Plan

1. Set up Gradle project with BitcoinJ dependency
2. Implement Wallet model class
3. Create WalletGenerator using BitcoinJ
4. Implement WalletImporter for private key/seed phrase import
5. Develop WalletService with core business logic
6. Create simple WalletStorage implementation
7. Build WalletCLI for user interaction
8. Write comprehensive unit tests
9. Create integration tests
10. Document usage and examples