package io.github.szabelin.dualchain.mint.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EthAddressTest {

    @Test
    void normalizes_mixed_case_to_lowercase() {
        EthAddress addr = new EthAddress("0xAbCdEf1234567890aBcDeF1234567890ABCDEF12");
        assertThat(addr.value()).isEqualTo("0xabcdef1234567890abcdef1234567890abcdef12");
    }

    @Test
    void accepts_all_lowercase() {
        EthAddress addr = new EthAddress("0x0000000000000000000000000000000000000001");
        assertThat(addr.value()).isEqualTo("0x0000000000000000000000000000000000000001");
    }

    @Test
    void toString_returns_value() {
        EthAddress addr = new EthAddress("0x0000000000000000000000000000000000000001");
        assertThat(addr.toString()).isEqualTo("0x0000000000000000000000000000000000000001");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0x",
            "0xabc",
            "abc",
            "0x123456789012345678901234567890123456789g",
            "1234567890123456789012345678901234567890",
            "0x123456789012345678901234567890123456789012"
    })
    void rejects_malformed(String input) {
        assertThatThrownBy(() -> new EthAddress(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Ethereum address");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejects_null_and_empty(String input) {
        assertThatThrownBy(() -> new EthAddress(input))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
