package io.github.szabelin.dualchain.mint.firefly;

import io.github.szabelin.dualchain.mint.domain.EthAddress;
import io.github.szabelin.dualchain.mint.firefly.dto.FireFlyMintRequest;
import io.github.szabelin.dualchain.mint.firefly.dto.FireFlyTokenBalance;
import io.github.szabelin.dualchain.mint.firefly.dto.FireFlyTokenPool;
import io.github.szabelin.dualchain.mint.firefly.dto.FireFlyTokenTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigInteger;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class FireFlyClient {

    private static final Logger log = LoggerFactory.getLogger(FireFlyClient.class);
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private final RestClient restClient;
    private final FireFlyProperties props;
    private volatile String resolvedPoolId;

    public FireFlyClient(RestClient fireFlyRestClient, FireFlyProperties props) {
        this.restClient = fireFlyRestClient;
        this.props = props;
    }

    public FireFlyTokenTransfer mint(EthAddress to, BigInteger amount) {
        FireFlyMintRequest body = new FireFlyMintRequest(
                poolId(), to.value(), amount.toString(),
                isBlank(props.signingKey()) ? null : props.signingKey()
        );
        log.debug("FireFly mint POST: pool={}, to={}, amount={}", body.pool(), to, amount);
        try {
            return restClient.post()
                    .uri("/api/v1/namespaces/{ns}/tokens/mint", props.namespace())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(FireFlyTokenTransfer.class);
        } catch (RestClientResponseException e) {
            throw new FireFlyException(e.getStatusCode().value(), e.getResponseBodyAsString(),
                    "FireFly mint failed: HTTP " + e.getStatusCode().value());
        }
    }

    public Optional<BigInteger> balance(EthAddress address) {
        String pool = poolId();
        try {
            FireFlyTokenBalance[] balances = restClient.get()
                    .uri(uri -> uri
                            .path("/api/v1/namespaces/{ns}/tokens/balances")
                            .queryParam("pool", pool)
                            .queryParam("key", address.value())
                            .build(props.namespace()))
                    .retrieve()
                    .body(FireFlyTokenBalance[].class);

            if (balances == null || balances.length == 0) {
                return Optional.empty();
            }
            return Optional.of(new BigInteger(balances[0].balance()));
        } catch (RestClientResponseException e) {
            throw new FireFlyException(e.getStatusCode().value(), e.getResponseBodyAsString(),
                    "FireFly balance lookup failed: HTTP " + e.getStatusCode().value());
        }
    }

    /**
     * FireFly's tokens/mint accepts pool name or UUID, but tokens/balances requires UUID.
     * We resolve once and cache, treating the configured value as a UUID if it looks like one.
     */
    private String poolId() {
        String cached = resolvedPoolId;
        if (cached != null) return cached;

        String configured = props.pool();
        if (UUID_PATTERN.matcher(configured).matches()) {
            resolvedPoolId = configured;
            return resolvedPoolId;
        }

        try {
            FireFlyTokenPool[] pools = restClient.get()
                    .uri(uri -> uri
                            .path("/api/v1/namespaces/{ns}/tokens/pools")
                            .queryParam("name", configured)
                            .build(props.namespace()))
                    .retrieve()
                    .body(FireFlyTokenPool[].class);

            if (pools == null || pools.length == 0) {
                throw new FireFlyException(404, "", "FireFly token pool not found: " + configured);
            }
            log.info("Resolved FireFly pool '{}' to id={}", configured, pools[0].id());
            resolvedPoolId = pools[0].id();
            return resolvedPoolId;
        } catch (RestClientResponseException e) {
            throw new FireFlyException(e.getStatusCode().value(), e.getResponseBodyAsString(),
                    "FireFly pool lookup failed: HTTP " + e.getStatusCode().value());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
