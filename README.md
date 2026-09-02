# Cashback Feature API

Fintech cashback API built as a Spring Boot modular monolith. PostgreSQL is the system of record for transactional data, Redis provides low-latency cache and idempotency controls, and Amazon Redshift Serverless stores analytics and historical archive data.

This repository contains application-level production hardening and a local deployment scaffold. Cloud identity, network, encryption, retention, and compliance evidence must still be configured and verified in staging or production.

## What is built

- User registration and role-based access with mutually exclusive USER, MERCHANT, ADMIN, and COMPLIANCE roles.
- Merchant registration, active cashback offers, expiry, and merchant-ID offer caching.
- Transaction processing with cashback calculation by transaction type.
- Cashback validation against spend and merchant payout limits.
- PostgreSQL-first reward balances and referral bonuses.
- Redis caching for balances, merchant offers, referral usage views, and transaction idempotency locks.
- Duplicate-credit protection using PostgreSQL plus an atomic Redis concurrency marker.
- Referral-code limits: 8,000 for local referrals and 10,000 for UK referrals.
- Audit events for negative balances, duplicate referrals, cashback anomalies, and payout inconsistencies.
- Durable PostgreSQL outbox with synchronous, retrying Redis and warehouse dispatch.
- Paged reconciliation, audit, cleanup, and warehouse extraction jobs.
- BigDecimal financial boundaries with request validation and database precision constraints.
- Owner-based authorization for user-scoped resources and role-protected administration endpoints.
- Write-only password input, server-assigned registration roles, sanitized API errors, and security headers.
- Incremental ETL from PostgreSQL to Redshift facts and daily reports.
- Redshift archive export before deleting old transactional rows.
- Quartz jobs for reconciliation, reporting, analytics, auditing, cleanup, settlement, partition creation, and archival.
- Docker Compose for the API, PostgreSQL, Redis, and a local warehouse substitute.

This is an API foundation, not a complete payment processor. Payment authorization, external settlement, refunds, chargebacks, identity verification, and a production-grade accounting ledger are outside the current scope.

## System workflow

### Transaction and cashback

```text
Client -> REST API -> validate spend, cashback, and payout
-> persist transaction in PostgreSQL
-> claim transaction marker in Redis
-> credit reward balance in PostgreSQL
-> enqueue cache and warehouse outbox events in PostgreSQL
-> retry outbox dispatch to Redis and warehouse after commit
```

PostgreSQL is the durable financial source of truth. Redis is an optimization and concurrency aid, never the ledger.

### Analytics and archive

```text
PostgreSQL transactional data
  -> incremental ETL and archive jobs
  -> Redshift Serverless facts, reports, and archive tables
  -> analytics APIs and BI tools
```

BI clients must query Redshift, not PostgreSQL.

### Referral bonus

```text
Referral code -> reserve bonus atomically in PostgreSQL
              -> check cumulative bonus limit
              -> persist reward and referral
              -> enqueue Redis usage refresh outbox event
              -> retry outbox dispatch after commit
```

## Architecture and modules

The application is one deployable Spring Boot process with explicit domain packages:

| Module | Responsibility |
| --- | --- |
| user | Accounts, roles, passwords, referral codes, balances, and balance-cache events. |
| transaction | Transaction intake, cashback rules, validation, idempotency, and transaction analytics. |
| merchant | Merchants, offers, active-offer lookup, offer caching, and offer analytics. |
| rewards | Reward records, reward credits, balance retrieval, and reward analytics. |
| referral | Referral registration, PostgreSQL-authoritative limits, and Redis usage views. |
| audit | Compliance events, anomaly recording, and protected audit endpoints. |
| analytics | Warehouse datasource, Redshift ETL, archive export, and warehouse queries. |
| scheduler | Quartz triggers and reconciliation, reporting, ETL, audit, cleanup, settlement, and archive jobs. |
| observability | Metrics, Prometheus configuration, dashboards, and OpenTelemetry integration. |

### Why a modular monolith

A modular monolith provides one deployment and local environment while preserving domain boundaries. It keeps PostgreSQL transactions straightforward, reduces distributed-systems overhead, simplifies testing and debugging, and leaves clear extraction points if a module later becomes a service.

The trade-off is a shared process, release cycle, JVM, and failure domain. Package boundaries, dependency rules, and module tests are needed to prevent tight coupling.

## API

The current API uses HTTP Basic authentication for local operation. Production deployments should replace it with OAuth2/OIDC and short-lived tokens through the selected identity provider.

### Users

```text
POST /api/users/register
GET  /api/users/{email}
```

Registration always creates a `USER` account. Privileged roles are assigned through an administrative provisioning process, not from public request data.

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ada@example.com",
    "password": "replace-me",
    "fullName": "Ada Example",
    "referralCode": "ADA123"
  }'
```

### Merchants and offers

```text
POST /api/merchants/register
POST /api/merchants/{merchantId}/offers
GET  /api/merchants/{merchantId}/offers
GET  /api/merchants/offers/analytics
```

```json
{
  "cashbackRate": 0.10,
  "expiryDate": "2026-12-31",
  "active": true
}
```

Offer lookup returns active, non-expired offers and uses `cashbackOffers:merchant:{merchantId}` when Redis is available.

### Transactions

```text
POST /api/transactions
GET  /api/transactions/user/{userId}
GET  /api/transactions/analytics
```

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "user": { "id": "22222222-2222-2222-2222-222222222222" },
  "merchant": { "id": "33333333-3333-3333-3333-333333333333" },
  "offer": { "id": "44444444-4444-4444-4444-444444444444" },
  "amount": 100.00,
  "type": "AIRTIME"
}
```

Clients must reuse the same transaction ID for retries. A persisted cashback amount makes a transaction ineligible for another credit.

### Rewards

```text
POST /api/rewards/credit?userId={userId}&amount=25.00&description=Promotion
GET  /api/rewards/user/{userId}
GET  /api/rewards/user/{userId}/balance
GET  /api/rewards/analytics
```

The balance endpoint is backed by Redis with PostgreSQL fallback.

### Referrals

```text
POST /api/referrals?referralCode=ADA123&referredUserId={userId}&isUKReferral=false
GET  /api/referrals/{referrerId}
```

### Audit

These endpoints require ADMIN or COMPLIANCE:

```text
GET /api/audit/events
GET /api/audit/events/{eventType}
GET /api/audit/events/id/{id}
```

### Actuator

```text
GET /actuator/health
GET /actuator/prometheus
```

## Data ownership

| Data | System of record | Consumers |
| --- | --- | --- |
| Users, roles, transactions, rewards, balances | PostgreSQL | API, security, reconciliation, ETL |
| Merchants and offers | PostgreSQL | API, offer cache, ETL |
| Audit events | PostgreSQL | Compliance API and anomaly checks |
| Balance and offer cache | Redis | Low-latency API reads; refreshed through the PostgreSQL outbox |
| Transaction idempotency markers | Redis plus PostgreSQL guard | Transaction processing |
| Analytics facts and reports | Redshift | Analytics APIs and BI |
| Historical archive | Redshift | Retention and analytical access |

Analytics services do not query transactional aggregate repositories. ETL is the controlled bridge from PostgreSQL to Redshift.

## Caching and idempotency

- User balances use `cashbackBalances:{userId}`. Committed PostgreSQL updates enqueue durable Redis refresh work; misses fall back to PostgreSQL.
- Merchant offers use `cashbackOffers:merchant:{merchantId}`. The TTL ends at the earliest offer expiry; unbounded offers use a ten-minute fallback.
- Transactions use `cashbackEligibility:processed:{transactionId}` with a configurable atomic marker between 24 and 48 hours, defaulting to 48 hours for daily reconciliation coverage. After the Redis marker expires, PostgreSQL remains the definitive check because a persisted cashback amount permanently rejects duplicate credit.
- Referral usage uses `referralBonus:code:{referralCode}` as a durable-outbox-backed view. PostgreSQL atomically reserves the bonus total and enforces the limit.

## Running locally

Prerequisites: Docker Desktop with Compose and Java 25 for development outside Docker.

```powershell
$env:POSTGRES_PASSWORD = "local-postgres-password"
$env:WAREHOUSE_PASSWORD = "local-warehouse-password"
docker compose up --build
```

The API is available at `http://localhost:8080`. Compose runs PostgreSQL, Redis, the Spring API, and a PostgreSQL container that emulates a separate warehouse locally. The local warehouse is not Amazon Redshift.

```powershell
docker compose down
.\gradlew.bat test --no-daemon
```

## Configuration

The application requires these variables at startup; there are no credential fallbacks:

```text
DB_USERNAME=postgres
DB_PASSWORD=change-me
REDSHIFT_NAMESPACE_ARN=<namespace-identifier>
AWS_REGION=eu-north-1
REDSHIFT_JDBC_URL=jdbc:redshift:iam://<workgroup-endpoint>:5439/<database>
REDSHIFT_DB_USERNAME=<warehouse-user>
REDSHIFT_DB_PASSWORD=change-me
WAREHOUSE_DATASOURCE_DRIVER_CLASS_NAME=com.amazon.redshift.jdbc42.Driver
```

Redis:

```text
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

For local Compose, the warehouse is PostgreSQL, so set `WAREHOUSE_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver` and use the Compose JDBC URL. For Redshift Serverless, use the IAM JDBC URL and a workgroup endpoint. The Redshift IAM JDBC scheme obtains temporary credentials from the AWS SDK credential provider chain; the runtime identity needs permission to connect to the workgroup and database user.

Business settings:

```text
cashback.transaction-eligibility-ttl=48h  # must be between 24h and 48h
cashback.referral-cache-ttl=30d
cashback.referral-local-max-bonus=8000
cashback.referral-uk-max-bonus=10000
```

Never commit passwords, access keys, tokens, or production connection strings. Use AWS Secrets Manager, Parameter Store, or workload identity.

## Migrations

Flyway runs `classpath:db/migration` against transactional PostgreSQL at startup and records applied versions in `flyway_schema_history`. Recent hardening migrations add the outbox, financial constraints, audit idempotency fingerprints, and the referral bonus counter (`V18`, `V19`, `V20`, and `V22`). The composite ETL watermark is maintained in the warehouse schema by `WarehouseEtlService`. Add new migrations with a higher version.

The warehouse schema is currently created and upgraded by ETL and archive services. Production should move warehouse DDL into a separately versioned migration process. The `prod` profile in `application.yml` uses a JDBC Quartz job store; provision the official Quartz PostgreSQL schema before enabling it.

## Observability and monitoring

Quartz jobs record the `jobs.executed` Micrometer counter with a job-name tag. Jobs include analytics, reporting, reconciliation, audit, partition creation, offer cleanup, settlement aggregation, and archival.

OpenTelemetry exports through OTLP. Monitor API latency/errors, PostgreSQL pool saturation and query latency, Redis hit rate/evictions, ETL row counts and watermark age, warehouse lag, cashback anomaly/rejection rates, duplicate attempts, referral-limit rejections, Quartz misfires, and Redshift queue/storage/concurrency metrics.

A healthy API with a stale warehouse or failed Redis refresh is still an operational incident.

## Compliance and security

Implemented controls include stateless role-based access, owner checks for user-scoped resources, write-only password input, server-assigned registration roles, request validation, database financial invariants, bounded audit queries, race-safe audit fingerprints, required secret configuration, SQL logging suppression, sanitized errors, security headers, and a non-root application container. These controls improve application readiness but are not a compliance certification.

### Partially covered in the repository

- Least-privilege application roles are defined for `USER`, `MERCHANT`, `ADMIN`, and `COMPLIANCE`; database, warehouse, and operational identities still need separate restricted roles.
- Required environment variables, SQL logging suppression, sanitized errors, owner checks, replay protection for transaction IDs, and audit anomaly events are implemented.
- Reconciliation jobs and manual-review audit events exist, but a double-entry ledger and independently controlled reconciliation process are still required for financial assurance.

### Required before production

- TLS for clients, PostgreSQL, Redis, and Redshift, with certificate rotation and verification.
- OAuth2/OIDC and short-lived tokens instead of local HTTP Basic authentication.
- IAM authentication and workload identity where supported.
- Managed secret storage with rotation and access auditing.
- Encryption at rest and in transit, including backups and snapshots.
- Minimal personal data in warehouse tables, with documented classification and masking rules.
- Restricted or immutable audit retention with retention locks and monitored deletion jobs.
- Centralized access logs for administrative and compliance endpoints, with alerting and retention.
- Documented retention, deletion, subject-access, incident-response, and restore procedures.
- Edge rate limiting and abuse detection; application-level transaction-ID replay protection is already implemented.
- Independent reconciliation of debits, cashback credits, and merchant payouts.
- Manual review workflow and evidence for anomalies, negative balances, duplicate referrals, and payout inconsistencies.

These controls require a staging or production environment, identity and network configuration, operational ownership, and audit evidence. Application code alone cannot demonstrate that they are active.

Application controls are not a compliance certification. Policies, evidence, access reviews, incident response, and independent assessment are also required.

## Trade-offs

- PostgreSQL provides correctness; Redis provides speed but can be stale or unavailable.
- Redshift protects OLTP workloads from BI scans but introduces ETL lag, duplicated schemas, and warehouse cost.
- A modular monolith reduces operational complexity but shares a process, release cycle, and failure domain.
- Financial entities, analytics mappings, and cache snapshots use `BigDecimal`; currency and explicit rounding rules remain part of the ledger hardening work.

## What can break first at scale

1. The ETL uses `(created_at, id)` watermarks; CDC is still preferable for very high throughput.
2. ETL still uses per-row warehouse `MERGE` operations; production should replace these with staging tables and bulk `COPY`.
3. Outbox delivery is at-least-once; cache writes and warehouse `MERGE` handlers must remain idempotent.
4. The atomic referral counter still serializes updates for one high-volume referrer; shard or ledger referral limits if that becomes a bottleneck.
5. The `prod` profile uses a JDBC Quartz store with clustering and misfire thresholds; the Quartz schema must be provisioned before startup.
6. Archive deletion is exact-ID and copy-count verified; restore drills, retention policies, and archival manifests are still required.
7. A local PostgreSQL warehouse substitute does not prove Redshift SQL compatibility; run a Redshift integration gate before release.

## Current scope and future work

- Add currency handling and explicit ledger rounding rules.
- Introduce a double-entry ledger with debit, credit, refund, chargeback, and settlement states.
- Replace page-level warehouse `MERGE` writes with staging tables and bulk `COPY` loads.
- Add versioned warehouse migrations and BI reporting views.
- Add PostgreSQL/Redis Testcontainers tests and Redshift integration coverage.
- Add broader endpoint contract and authorization tests.
- Add structured logs, correlation IDs, distributed tracing, rate limiting, and security scanning.
- Add backups, restore drills, disaster recovery, runbooks, and retention automation.
- Replace HTTP Basic with managed OAuth2/OIDC authentication.

## License

No license is currently specified in this repository.
