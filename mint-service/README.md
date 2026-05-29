# mint-service

Spring Boot 3.4 service that mints `DUAL` ERC-20 tokens on a permissioned Besu chain via Hyperledger FireFly's REST gateway. The private rail of [dual-chain-on-firefly](../README.md).

## Why this exists

The service is intentionally tiny — two endpoints, no database, no state. The point is to show what a clean Spring Boot + FireFly integration looks like when you're not using `web3j` and not writing your own chain code.

Specifically:

- **No `web3j` / `ethers`.** FireFly speaks REST + OpenAPI. The service is a plain HTTP client (`RestClient`).
- **No chain awareness in the controller layer.** `MintController` deals in addresses and BigIntegers; it has no idea Besu exists.
- **The FireFly client is the only boundary.** Everything chain-related is wrapped in `FireFlyException` so callers handle one error type, not a zoo of chain SDK exceptions.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/mints` | Mint `amount` of `DUAL` to `to`. Returns 202 with the FireFly operation ID + transaction ID. |
| `GET`  | `/api/v1/balances/{address}` | Current `DUAL` balance of `address` (0 if no minted history). |
| `GET`  | `/actuator/health` | Spring Boot health. |

### POST /api/v1/mints

```http
POST /api/v1/mints
Content-Type: application/json

{
  "to": "0x81A2C7B04aB3833e1E1a0bcb7ce01f005a68995c",
  "amount": "1000000000000000000"
}
```

```http
HTTP/1.1 202 Accepted
Content-Type: application/json

{
  "operationId":   "b9626da0-6530-49fb-a2aa-5592fffdb22a",
  "to":            "0x81a2c7b04ab3833e1e1a0bcb7ce01f005a68995c",
  "amount":        "1000000000000000000",
  "transactionId": "c450e833-fda8-4d70-aa52-638041f59e11",
  "status":        "SUBMITTED"
}
```

Validation errors return RFC-7807 `application/problem+json` with a 400.
Upstream FireFly errors get wrapped as a 502 with the upstream status in the body.

### GET /api/v1/balances/{address}

```http
GET /api/v1/balances/0x81A2C7B04aB3833e1E1a0bcb7ce01f005a68995c
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{ "address": "0x81a2c7b04ab3833e1e1a0bcb7ce01f005a68995c", "balance": "2000000000000000000" }
```

`amount` and `balance` are decimal strings in token base units (no decimals applied). A "1.0 DUAL" balance is `"1000000000000000000"` because `DUAL` has 18 decimals — same as ETH.

## Architecture

```
api/        controllers, DTOs, validation, RFC-7807 error handler, correlation-id filter
  └── MintController, BalanceController, GlobalExceptionHandler, CorrelationIdFilter
domain/     value objects, business rules
  └── EthAddress, MintReceipt, MintService
firefly/    external boundary
  ├── FireFlyClient        (RestClient → FireFly REST gateway)
  ├── FireFlyProperties    (@ConfigurationProperties record)
  ├── FireFlyException     (wrapped upstream errors)
  └── dto/                 (wire types, Jackson @JsonIgnoreProperties on everything)
config/
  └── RestClientConfig     (timeout-bounded RestClient bean)
```

A few small choices worth mentioning:

- **`EthAddress` is a record with a compact constructor** that validates + normalizes (lowercases) the input. Constructing one is the validation — there is no "valid but unchecked" state.
- **Pool name → UUID is resolved lazily and cached** in `FireFlyClient`. FireFly's `tokens/mint` accepts either a pool name or a UUID, but `tokens/balances` only accepts a UUID. Resolving on first use means you can configure `firefly.pool: DualToken` in your YAML and it Just Works.
- **`@MockitoBean` everywhere** (Spring Framework 6.2 / Spring Boot 3.4) instead of the deprecated `@MockBean`.
- **Virtual threads on** (`spring.threads.virtual.enabled=true`) since this is a Java 21 service and Tomcat 10.1 plays well with them.
- **Correlation IDs** via a servlet filter, propagated to the response header and into the SLF4J MDC. Useful when you start chaining services together.

## Configuration

| Property | Default | Notes |
|---|---|---|
| `firefly.base-url` | `http://localhost:5100` | FireFly Core REST API. |
| `firefly.namespace` | `default` | FireFly namespace. |
| `firefly.pool` | `DualToken` | Pool name or UUID. Name is resolved lazily. |
| `firefly.signing-key` | *(empty)* | Org wallet address used to sign mint transactions. If blank, FireFly's namespace-default key is used. |
| `firefly.connect-timeout` | `5s` | RestClient connect timeout. |
| `firefly.read-timeout` | `15s` | RestClient read timeout. |
| `server.port` | `8090` | (FireFly's data-exchange container squats on 8080.) |

Override any of these via env vars, command-line args, or a profile-specific YAML. For example:

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--firefly.signing-key=0xYOUR_KEY --firefly.pool=MyPool"
```

## Running tests

```bash
./mvnw test       # 30 unit tests (~2s, no network)
./mvnw verify     # also runs the 4 integration tests (~5s, WireMock)
```

The integration tests stand up a full Spring Boot context and a WireMock server stubbing FireFly. They run without Docker, FireFly, or any external service.

## Live smoke test

If you actually want to mint against a running FireFly stack, follow the [root README's "How to run it yourself"](../README.md#how-to-run-it-yourself) section.
