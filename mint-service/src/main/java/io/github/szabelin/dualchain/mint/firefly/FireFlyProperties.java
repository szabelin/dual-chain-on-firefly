package io.github.szabelin.dualchain.mint.firefly;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "firefly")
public record FireFlyProperties(
        @NotBlank String baseUrl,
        @NotBlank String namespace,
        @NotBlank String pool,
        String signingKey,
        Duration connectTimeout,
        Duration readTimeout
) {
    public FireFlyProperties {
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(5);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(15);
    }
}
