package io.github.szabelin.dualchain.mint.firefly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FireFlyTokenPool(
        String id,
        String name,
        String namespace,
        String symbol,
        Boolean active
) {}
