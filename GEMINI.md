# BTC Wallet Management App

## Project Overview

This is a comprehensive Bitcoin wallet management application built with Java 21 and the BitcoinJ library. It adheres to a layered architecture, separating the command-line interface (CLI) from core business services. The application supports standard wallet operations including hierarchical deterministic (HD) wallet generation, WIF/mnemonic import, UTXO-based balance tracking, and transaction creation/broadcasting.

**Key Technologies:**
*   **Language:** Java 21
*   **Build System:** Gradle
*   **Bitcoin Library:** BitcoinJ 0.16.3
*   **Testing:** JUnit 5, Mockito

**Core Architecture:**
The system is organized into distinct services:
*   `WalletService`: Manages wallet lifecycle (creation, import, validation).
*   `TransactionService`: Handles transaction construction (UTXO selection, inputs/outputs), signing, and broadcasting.
*   `BalanceService`: Fetches and caches wallet balances and UTXOs.
*   `BitcoinNodeClient`: Low-level interface for blockchain interaction.

## Building and Running

The project uses the Gradle Wrapper for consistent build execution.

**Build the Project:**
```bash
./gradlew build
```

**Run the Application:**
```bash
./gradlew run
```

**Execute Tests:**
```bash
./gradlew test
```

## Development Conventions

### Coding Style & Patterns
*   **Functional Programming:** Prefer Java Streams (`map`, `filter`, `reduce`) and `Optional` over null checks and imperative loops, unless specific index access is required (e.g., transaction signing).
*   **Immutability:** Use immutable data structures where possible. The `Wallet` model is implemented as a Java `record`.
*   **Pure Functions:** Strive for pure functions that avoid side effects, especially in service logic.
*   **Simplicity:** Simplicity and readability over abstractions.
*   **DDD:** Domain Driven Design is key.


### Transaction Handling
*   **UTXO Management:** Transactions must explicitly select Unspent Transaction Outputs (UTXOs) from the `BalanceService`.
*   **Change Outputs:** Always calculate and include a change output if the input surplus exceeds the dust limit.
*   **Signing:** Use `SigHash.ALL` to ensure transaction integrity.

### Error Handling
*   **Checked Exceptions:** Use specific, custom checked exceptions (`TransactionException`, `WalletException`, `BalanceException`) to handle errors gracefully.
*   **No Unchecked Exceptions:** Avoid throwing raw RuntimeExceptions in core business logic. Catch and wrap them in the appropriate custom exception.

### Testing Guidelines
*   **Frameworks:** Use JUnit 5 and Mockito.
*   **Exception Testing:** Tests should not declare `throws Exception`. Instead, use `assertThrows` or `try-catch` blocks to assert failure conditions explicitly.
*   **Mocking:** extensively mock external dependencies (like `BitcoinNodeClient` and `BalanceService`) to isolate unit tests.
