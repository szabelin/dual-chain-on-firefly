package io.github.szabelin.dualchain.mint.firefly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FireFlyMintRequest(
        String pool,
        String to,
        String amount,
        String key
) {}
