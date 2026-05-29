package io.github.szabelin.dualchain.mint.firefly;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.szabelin.dualchain.mint.domain.EthAddress;
import io.github.szabelin.dualchain.mint.firefly.dto.FireFlyTokenTransfer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FireFlyClientTest {

    private static final EthAddress ADDR = new EthAddress("0x0000000000000000000000000000000000000001");
    private static final String POOL_UUID = "11111111-2222-3333-4444-555555555555";

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @AfterEach
    void resetStubs() {
        wireMock.resetAll();
    }

    private static FireFlyClient client(String poolConfig) {
        FireFlyProperties props = new FireFlyProperties(
                "http://localhost:" + wireMock.port(),
                "default",
                poolConfig,
                "0xabc",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5)
        );
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) props.connectTimeout().toMillis());
        factory.setReadTimeout((int) props.readTimeout().toMillis());
        RestClient restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();
        return new FireFlyClient(restClient, props);
    }

    @Test
    void mint_uses_configured_pool_uuid_directly() {
        wireMock.stubFor(post("/api/v1/namespaces/default/tokens/mint")
                .willReturn(okJson("""
                        {
                            "localId": "op-1",
                            "pool": "%s",
                            "key": "0xkey",
                            "to": "%s",
                            "amount": "100",
                            "tx": { "type": "token_transfer", "id": "tx-1" }
                        }
                        """.formatted(POOL_UUID, ADDR.value()))));

        FireFlyTokenTransfer transfer = client(POOL_UUID).mint(ADDR, BigInteger.valueOf(100));

        assertThat(transfer.localId()).isEqualTo("op-1");
        assertThat(transfer.tx().id()).isEqualTo("tx-1");

        wireMock.verify(postRequestedFor(urlEqualTo("/api/v1/namespaces/default/tokens/mint"))
                .withRequestBody(matchingJsonPath("$.pool", equalTo(POOL_UUID)))
                .withRequestBody(matchingJsonPath("$.to", equalTo(ADDR.value())))
                .withRequestBody(matchingJsonPath("$.amount", equalTo("100")))
                .withRequestBody(matchingJsonPath("$.key", equalTo("0xabc"))));
    }

    @Test
    void name_is_resolved_to_uuid_and_cached() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/namespaces/default/tokens/pools"))
                .withQueryParam("name", equalTo("DualToken"))
                .willReturn(okJson("""
                        [{"id":"%s","name":"DualToken","namespace":"default","symbol":"DUAL","active":true}]
                        """.formatted(POOL_UUID))));
        wireMock.stubFor(post("/api/v1/namespaces/default/tokens/mint")
                .willReturn(okJson("""
                        {"localId":"op-1","pool":"%s","key":"0xkey","to":"%s","amount":"1","tx":null}
                        """.formatted(POOL_UUID, ADDR.value()))));

        FireFlyClient c = client("DualToken");
        c.mint(ADDR, BigInteger.ONE);
        c.mint(ADDR, BigInteger.ONE);

        // Pool lookup happens exactly once (cached for the second call)
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/api/v1/namespaces/default/tokens/pools")));
        // Both mints sent the resolved UUID, not the name
        wireMock.verify(2, postRequestedFor(urlEqualTo("/api/v1/namespaces/default/tokens/mint"))
                .withRequestBody(matchingJsonPath("$.pool", equalTo(POOL_UUID))));
    }

    @Test
    void unknown_pool_name_throws_FireFlyException() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/namespaces/default/tokens/pools"))
                .willReturn(okJson("[]")));

        assertThatThrownBy(() -> client("UnknownPool").mint(ADDR, BigInteger.ONE))
                .isInstanceOf(FireFlyException.class)
                .hasMessageContaining("UnknownPool");
    }

    @Test
    void mint_4xx_is_wrapped_as_FireFlyException() {
        wireMock.stubFor(post("/api/v1/namespaces/default/tokens/mint")
                .willReturn(badRequest().withBody("{\"error\":\"bad pool\"}")));

        assertThatThrownBy(() -> client(POOL_UUID).mint(ADDR, BigInteger.TEN))
                .isInstanceOf(FireFlyException.class)
                .satisfies(ex -> {
                    FireFlyException ffe = (FireFlyException) ex;
                    assertThat(ffe.getStatus()).isEqualTo(400);
                    assertThat(ffe.getBody()).contains("bad pool");
                });
    }

    @Test
    void mint_5xx_is_wrapped_as_FireFlyException() {
        wireMock.stubFor(post("/api/v1/namespaces/default/tokens/mint")
                .willReturn(serverError()));

        assertThatThrownBy(() -> client(POOL_UUID).mint(ADDR, BigInteger.TEN))
                .isInstanceOf(FireFlyException.class);
    }

    @Test
    void balance_returns_first_match() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/namespaces/default/tokens/balances"))
                .withQueryParam("pool", equalTo(POOL_UUID))
                .withQueryParam("key", equalTo(ADDR.value()))
                .willReturn(okJson("""
                        [
                          {"pool":"%s","key":"%s","balance":"1000000000000000000"}
                        ]
                        """.formatted(POOL_UUID, ADDR.value()))));

        Optional<BigInteger> balance = client(POOL_UUID).balance(ADDR);

        assertThat(balance).contains(new BigInteger("1000000000000000000"));
    }

    @Test
    void balance_returns_empty_for_no_results() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/namespaces/default/tokens/balances"))
                .willReturn(okJson("[]")));

        assertThat(client(POOL_UUID).balance(ADDR)).isEmpty();
    }

    @Test
    void balance_5xx_is_wrapped_as_FireFlyException() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/namespaces/default/tokens/balances"))
                .willReturn(notFound()));

        assertThatThrownBy(() -> client(POOL_UUID).balance(ADDR))
                .isInstanceOf(FireFlyException.class);
    }
}
