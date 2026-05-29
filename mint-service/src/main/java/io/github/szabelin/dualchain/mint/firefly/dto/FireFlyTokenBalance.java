package io.github.szabelin.dualchain.mint.firefly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FireFlyTokenBalance(
        String pool,
        String key,
        String balance
) {}
