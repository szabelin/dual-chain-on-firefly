# dual-chain-on-firefly

A small crypto sandbox: one web app, one wallet, two blockchain rails.

- **Public rail** — the browser signs an ETH transfer and broadcasts it to **Ethereum mainnet** via Alchemy.
- **Private rail** — a Java service hits a local **Hyperledger FireFly** REST gateway, which mints an ERC-20 on a permissioned **Besu** chain.

Same UI, same wallet (Privy embedded, email login), two completely different settlement layers. I built it to play with what public + permissioned chains feel like when they live behind a single user experience.

## Architecture

![architecture](diagrams/architecture.png)

## Stack

| Layer | Tech |
|---|---|
| Frontend | Next.js 15, TypeScript, Tailwind, shadcn/ui |
| Auth + wallet | Privy (embedded wallet, email login) |
| Public RPC | Alchemy (Ethereum mainnet) |
| Backend | Java 21, Spring Boot 3.4, RestClient (no web3 libs) |
| Private chain stack | Hyperledger FireFly → evmconnect → Besu (Clique PoA) |
| Contract | `DualToken.sol` — OpenZeppelin ERC-20, 18 decimals, symbol `DUAL` |

The Java service intentionally uses plain HTTP to talk to FireFly — no `web3j`, no `ethers`. FireFly's REST gateway abstracts the chain.

## How it works

**Public rail.** The Privy SDK signs a tx in the browser using the user's embedded wallet (MPC-secured key shares — no extension, no seed phrase). The signed tx is broadcast to Alchemy's mainnet RPC and lands on-chain like any other ETH transfer.

**Private rail.** The web app calls the Java service. The Java service POSTs to FireFly's REST gateway (`localhost:5100`). FireFly's evmconnect plugin assembles a Besu tx, signs it with the org wallet, and submits it to the local permissioned chain. The Transfer event flows back through FireFly as a blockchain event.

**Both rails feed back into the UI** over WebSockets so transactions show up live without polling.

## Status

Personal exploration project. Pieces are wired up and validated end-to-end on my machine (FireFly stack healthy, mainnet RPC reachable, a `DUAL` mint and an ETH transfer both confirmed on their respective chains). The code is a sandbox, not a library — fork it, rip it apart, point it at your own keys.

## License

MIT
