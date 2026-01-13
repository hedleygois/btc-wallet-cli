# BTC Wallet Management App Architecture

## Overview
A simple Bitcoin wallet management application using BitcoinJ library. The application provides basic wallet functionality including wallet generation, display of public addresses, and import of existing wallets.

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

#### 4. Wallet Storage
- **Responsibility**: Persistent storage of wallet information
- **Implementation**: Simple file-based storage (JSON format)
- **Data**: Wallet address, public key, encrypted private key

#### 5. User Interface
- **Responsibility**: Command-line interface for user interaction
- **Functions**:
  - Display menu options
  - Handle user input
  - Display wallet information
  - Show error messages

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

### Technology Stack

- **Language**: Java 11+
- **Build System**: Gradle
- **Bitcoin Library**: BitcoinJ
- **Testing**: JUnit 5
- **Storage**: JSON files

### Class Structure

```
com.btcwallet
├── Main.java                          # Entry point
├── service
│   ├── WalletService.java             # Core wallet operations
│   ├── WalletGenerator.java           # Wallet creation
│   └── WalletImporter.java            # Wallet import
├── model
│   └── Wallet.java                    # Wallet data model
├── storage
│   └── WalletStorage.java             # Persistence layer
├── cli
│   └── WalletCLI.java                 # Command line interface
└── exception
    └── WalletException.java           # Custom exceptions
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

### Testing Strategy

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **Mocking**: Use mock objects for external dependencies (BitcoinJ)
4. **Test Coverage**: Aim for 80%+ code coverage

### Future Extensions

1. **Transaction Support**: Send/receive BTC functionality
2. **Multi-Wallet Support**: Manage multiple wallets
3. **Balance Checking**: Query blockchain for wallet balance
4. **QR Code Support**: Generate QR codes for addresses
5. **Hardware Wallet Integration**: Support for hardware wallets

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