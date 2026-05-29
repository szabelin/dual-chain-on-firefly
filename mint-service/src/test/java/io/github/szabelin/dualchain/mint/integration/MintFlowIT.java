package io.github.szabelin.dualchain.mint.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MintFlowIT {

    private static final String ADDR = "0x0000000000000000000000000000000000000001";

    private static WireMockServer wireMock;

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().port(18089));
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

    @Test
    void mint_endpoint_calls_firefly_and_returns_receipt() {
        wireMock.stubFor(post("/api/v1/namespaces/default/tokens/mint")
                .willReturn(okJson("""
                        {
                          "localId": "op-1",
                          "pool": "pool-uuid",
                          "key": "0xkey",
                          "to": "%s",
                          "amount": "1000000000000000000",
                          "tx": {"type": "token_transfer", "id": "tx-uuid"}
                        }
                        """.formatted(ADDR))));

        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> resp = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) rest.postForEntity(
                "http://localhost:" + port + "/api/v1/mints",
                Map.of("to", ADDR, "amount", "1000000000000000000"),
                Map.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("operationId")).isEqualTo("op-1");
        assertThat(resp.getBody().get("transactionId")).isEqualTo("tx-uuid");
        assertThat(resp.getBody().get("status")).isEqualTo("SUBMITTED");

        wireMock.verify(postRequestedFor(urlEqualTo("/api/v1/namespaces/default/tokens/mint"))
                .withRequestBody(matchingJsonPath("$.pool", equalTo("11111111-2222-3333-4444-555555555555")))
                .withRequestBody(matchingJsonPath("$.to", equalTo(ADDR))));
    }

    @Test
    void balance_endpoint_queries_firefly() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/namespaces/default/tokens/balances"))
                .willReturn(okJson("""
                        [{"pool":"uuid","key":"%s","balance":"42"}]
                        """.formatted(ADDR))));

        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> resp = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) rest.getForEntity(
                "http://localhost:" + port + "/api/v1/balances/" + ADDR,
                Map.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("address")).isEqualTo(ADDR);
        assertThat(resp.getBody().get("balance")).isEqualTo("42");
    }

    @Test
    void upstream_5xx_returns_502() {
        wireMock.stubFor(post("/api/v1/namespaces/default/tokens/mint")
                .willReturn(serverError()));

        ResponseEntity<String> resp = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/mints",
                Map.of("to", ADDR, "amount", "1"),
                String.class
        );

        assertThat(resp.getStatusCode().value()).isEqualTo(502);
    }

    @Test
    void invalid_address_returns_400_without_calling_firefly() {
        ResponseEntity<String> resp = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/mints",
                Map.of("to", "nope", "amount", "1"),
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(wireMock.getAllServeEvents()).isEmpty();
    }
}
