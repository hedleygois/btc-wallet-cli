package com.btcwallet.transaction.dto;

import com.btcwallet.transaction.Transaction;
import java.time.Instant;
import java.math.BigDecimal;

public record TransactionDTO(
    String transactionId,
    String walletId,
    String recipientAddress,
    BigDecimal amountBtc,
    BigDecimal feeBtc,
    long totalSatoshis,
    String status,
    boolean simulation,
    Instant createdAt
) {
    public static TransactionDTO fromTransaction(Transaction transaction) {
        return new TransactionDTO(
            transaction.transactionId(),
            transaction.walletId(),
            transaction.recipientAddress(),
            transaction.getAmountAsBigDecimal(),
            transaction.getFeeAsBigDecimal(),
            transaction.getTotalAmount(),
            transaction.status().name(),
            transaction.isSimulation(),
            transaction.createdAt()
        );
    }
}
