package io.github.szabelin.dualchain.mint.domain;

import java.math.BigInteger;

public record MintReceipt(
        String operationId,
        EthAddress to,
        BigInteger amount,
        String transactionId
) {}
