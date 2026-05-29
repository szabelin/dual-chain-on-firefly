package io.github.szabelin.dualchain.mint.api.dto;

import io.github.szabelin.dualchain.mint.domain.MintReceipt;

public record MintResponse(
        String operationId,
        String to,
        String amount,
        String transactionId,
        String status
) {
    public static MintResponse from(MintReceipt receipt) {
        return new MintResponse(
                receipt.operationId(),
                receipt.to().value(),
                receipt.amount().toString(),
                receipt.transactionId(),
                "SUBMITTED"
        );
    }
}
