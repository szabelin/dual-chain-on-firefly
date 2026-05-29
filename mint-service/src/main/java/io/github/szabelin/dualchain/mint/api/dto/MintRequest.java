package io.github.szabelin.dualchain.mint.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigInteger;

public record MintRequest(
        @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "must be a 0x-prefixed 40-hex Ethereum address")
        String to,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigInteger amount
) {}
