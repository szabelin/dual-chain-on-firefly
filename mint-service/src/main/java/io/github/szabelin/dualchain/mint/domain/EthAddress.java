package io.github.szabelin.dualchain.mint.domain;

import java.util.regex.Pattern;

public record EthAddress(String value) {

    private static final Pattern PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    public EthAddress {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid Ethereum address: " + value);
        }
        value = value.toLowerCase();
    }

    @Override
    public String toString() {
        return value;
    }
}
