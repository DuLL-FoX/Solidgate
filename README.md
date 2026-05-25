# Solidgate test automation

Java 17, Maven, Selenide, JUnit 5. Two tests for the Solidgate payment flow, sharing one order initialized in `@BeforeAll`:

- `payOrderViaPaymentPage`: opens the Payment Page in Chrome, fills the card in the PCI iframe, waits for the success screen.
- `statusEndpointReflectsSuccessfulPayment`: polls `/api/v1/status` for that order and checks `amount`, `currency`, approved status, and at least one successful transaction.

## Run

```bash
cp .env.example .env
# fill SOLIDGATE_PUBLIC_KEY / SOLIDGATE_SECRET_KEY in .env
./mvnw test -Pheadless
```

Env vars also work without `.env`:

```bash
export SOLIDGATE_PUBLIC_KEY=...
export SOLIDGATE_SECRET_KEY=...
./mvnw test -Pheadless
```

See [.env.example](.env.example) for the full list of variables. Maven Wrapper is committed, no system Maven required.

## CI

[.github/workflows/tests.yml](.github/workflows/tests.yml) is manual-trigger only (`workflow_dispatch`). Add `SOLIDGATE_PUBLIC_KEY` and `SOLIDGATE_SECRET_KEY` as GitHub Actions secrets.
