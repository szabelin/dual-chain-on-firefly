package io.github.szabelin.dualchain.mint.domain;

import io.github.szabelin.dualchain.mint.firefly.FireFlyClient;
import io.github.szabelin.dualchain.mint.firefly.dto.FireFlyTokenTransfer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MintServiceTest {

    private static final EthAddress ADDR = new EthAddress("0x0000000000000000000000000000000000000001");

    @Mock FireFlyClient fireFly;
    @InjectMocks MintService service;

    @Test
    void mints_via_firefly_and_returns_receipt() {
        BigInteger amount = new BigInteger("1000000000000000000");
        FireFlyTokenTransfer transfer = new FireFlyTokenTransfer(
                "op-1", "pool-uuid", "0xkey", ADDR.value(), amount.toString(),
                new FireFlyTokenTransfer.Tx("token_transfer", "tx-1")
        );
        when(fireFly.mint(ADDR, amount)).thenReturn(transfer);

        MintReceipt receipt = service.mint(ADDR, amount);

        assertThat(receipt.operationId()).isEqualTo("op-1");
        assertThat(receipt.to()).isEqualTo(ADDR);
        assertThat(receipt.amount()).isEqualTo(amount);
        assertThat(receipt.transactionId()).isEqualTo("tx-1");
    }

    @Test
    void handles_missing_tx_block() {
        FireFlyTokenTransfer transfer = new FireFlyTokenTransfer(
                "op-2", "pool", "key", ADDR.value(), "1", null
        );
        when(fireFly.mint(ADDR, BigInteger.ONE)).thenReturn(transfer);

        MintReceipt receipt = service.mint(ADDR, BigInteger.ONE);

        assertThat(receipt.transactionId()).isNull();
    }

    @Test
    void rejects_zero_amount() {
        assertThatThrownBy(() -> service.mint(ADDR, BigInteger.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_negative_amount() {
        assertThatThrownBy(() -> service.mint(ADDR, BigInteger.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null_amount() {
        assertThatThrownBy(() -> service.mint(ADDR, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
