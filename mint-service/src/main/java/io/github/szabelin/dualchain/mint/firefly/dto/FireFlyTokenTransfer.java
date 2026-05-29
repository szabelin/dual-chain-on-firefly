package io.github.szabelin.dualchain.mint.firefly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FireFlyTokenTransfer(
        String localId,
        String pool,
        String key,
        String to,
        String amount,
        Tx tx
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tx(String type, String id) {}
}
