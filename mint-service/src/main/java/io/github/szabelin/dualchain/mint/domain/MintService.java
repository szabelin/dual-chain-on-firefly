package io.github.szabelin.dualchain.mint.domain;

import io.github.szabelin.dualchain.mint.firefly.FireFlyClient;
import io.github.szabelin.dualchain.mint.firefly.dto.FireFlyTokenTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
public class MintService {

    private static final Logger log = LoggerFactory.getLogger(MintService.class);

    private final FireFlyClient fireFly;

    public MintService(FireFlyClient fireFly) {
        this.fireFly = fireFly;
    }

    public MintReceipt mint(EthAddress to, BigInteger amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be a positive integer");
        }
        log.info("Submitting mint: to={}, amount={}", to, amount);
        FireFlyTokenTransfer transfer = fireFly.mint(to, amount);
        String txId = transfer.tx() != null ? transfer.tx().id() : null;
        log.info("Mint submitted: operationId={}, transactionId={}", transfer.localId(), txId);
        return new MintReceipt(transfer.localId(), to, amount, txId);
    }
}
