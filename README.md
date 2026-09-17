# Real-Time Anomaly Detection System

A containerized, event-driven system written in **Java 21** that generates continuous numerical metrics, streams them via **RabbitMQ**, and identifies statistical outliers in real time using a rolling window $z$-score algorithm.

---

## Architectural Overview

- RabbitMQ 3.13 (Direct Exchange)
- Producer Service (Spring Boot 3.5, Gaussian PRNG, Outlier Inject)
- Consumer Service (Spring Boot 3.5, Ring Buffer, Rolling Z-Score)


### Components

1. **Messaging System (`rabbitmq:3.13-management-alpine`):**
    * Uses AMQP 0-9-1 with durable queues.
    * Container health checks (`rabbitmq-diagnostics`) ensure dependent services start only when the broker is operational.
    * Includes web management interface at port `15672`.

2. **Producer Service:**
    * Emits floating-point values drawn from a Gaussian distribution $\mathcal{N}(\mu=50, \sigma=5)$ every 200ms.
    * Intentionally injects statistical anomalies with a 5% probability at $\approx \pm 6\sigma$.
    * Utilizes modern Java PRNG architecture (`RandomGenerator`) with fallback mechanisms.

3. **Consumer Service:**
    * Ingests data points asynchronously over virtual threads.
    * Maintains a rolling circular ring buffer of capacity $N=100$.
    * Applies a rolling standard deviation statistical filter.

---

## Statistical Model & Anomaly Detection Logic

### Rolling Window Algorithm

To process continuous streams without unbounded memory growth or object allocation overhead, the consumer maintains a thread-safe circular array (ring buffer):

* **Sample Mean ($\mu$):**
  $$\mu = \frac{1}{n}\sum_{i=1}^n x_i$$
* **Sample Standard Deviation ($\sigma$):**
  $$\sigma = \sqrt{\frac{1}{n - 1}\sum_{i=1}^n (x_i - \mu)^2}$$
* **Standard Score ($z$):**
  $$z = \frac{x - \mu}{\sigma}$$

### Key Engineering Decisions

* **Warm-up Phase:** Points arriving before the buffer reaches `min-sample-size` (50 items) are treated as training 
  data and do not trigger false alerts.
* **Contamination Protection (Anti-Poisoning):** If an incoming observation is classified as an anomaly ($\vert{}z\vert{} \ge 3.0$), it is logged and **discarded from the rolling window**. This prevents severe outliers from skewing subsequent mean and variance baselines.

---

## Prerequisites

* **Docker Engine** 24+ and **Docker Compose** v2+
* *(Optional for local host compilation)*: JDK 21 and Maven 3.9+

---

## Quick Start (Docker Compose)

### Run the entire system
From the repository root:

```bash
docker compose up --build
```

### Inspect logs
```bash
docker compose logs -f consumer
```

### Management UI
```bash
http://localhost:15672
```
---

### Configuration Reference

Both services expose environment variables for tuning via docker-compose.yml

---

### Final Checklist for Review

* Multi-module Maven setup builds cleanly: `mvn clean package -DskipTests`.
* Unit tests pass: `mvn clean test`.
* No host dependencies required: `docker compose up --build` works out of the box.
* Healthy startup ordering guarantees no connection retry crashes.
* Minimal multi-stage Docker images (`eclipse-temurin:21-jre-alpine`) run as a non-root user.

---

## Up Next & Production Readiness Roadmap

This submission prioritizes a clean, zero-dependency out-of-the-box local setup. If transitioning this proof-of-concept into an enterprise-grade production environment, the following areas would be addressed:

### 1. Developer Tooling, Quality & CI/CD
* **Testing Stack:**
    * **Testcontainers:** Introduce `@Testcontainers` to replace embedded mocks with real RabbitMQ containers for integration tests inside the CI pipeline (`mvn verify`).
    * **Contract Testing:** Use Spring Cloud Contract or Pact between Producer and Consumer to prevent breaking changes in shared event schemas.
* **Code Quality & Linting:**
    * **Checkstyle:** Automated code formatting and style validation enforced at build time.
    * **Dependabot / Renovate:** Automated dependency updates and CVE patching.
* **API:**
    * **OpenAPI / Swagger:** If REST endpoints are added (e.g., administrative triggers or query endpoints).

---

### 2. Kubernetes & Orchestration Readiness
To deploy this system reliably to Kubernetes, the following configurations would be added:

* **Health Probes:** Add `spring-boot-starter-actuator` to expose `/actuator/health/liveness` and `/actuator/health/readiness` endpoints, mapped to K8s `livenessProbe` and `readinessProbe`.
* **Graceful Termination:** Configure `server.shutdown=graceful` and handle AMQP channel drain timeouts so consumer pods don't drop mid-flight messages on scale-down or rolling deployments.
* **Config & Secret Management:** Externalize RabbitMQ credentials and statistical hyper-parameters via `ConfigMap` and `Secret` resources or HashiCorp Vault.
* **Autoscaling (KEDA):** Consumer scaling cannot rely on CPU/Memory metrics alone. Integrate **KEDA (Kubernetes Event-driven Autoscaling)** to scale Consumer pods dynamically based on RabbitMQ queue depth (`metrics.datapoints.queue` lag).
* **Package Management:** Package deployments as Helm charts for multi-environment promotion (dev/stage/prod).

---

### 3. Missing Technical Requirements & Architectural Gaps

* **Stateless Consumer & Distributed State (Critical Limitation):**
    * *Current State:* The rolling statistical window is held in an in-memory ring buffer.
    * *Limitation:* If the Consumer scales horizontally (multiple replicas) or crashes, the rolling history is lost or split across replicas, causing partition skew and false anomalies.
    * *Solution:* Shift window computation to a distributed streaming engine (Kafka Streams).
* **Dead Letter Queue (DLQ) & Poison Pill Handling:**
    * Malformed JSON payloads or unhandled exceptions currently risk rejecting or repeatedly retrying messages. A production setup requires dead-letter exchanges (`x-dead-letter-exchange`) and retry backoff policies.
* **Observability & Alerting:**
    * Export Micrometer metrics to Prometheus (message ingest rates, z-score distributions, anomaly counters).
    * Instrument OpenTelemetry distributed tracing (W3C traceparent injected in AMQP headers) to track end-to-end latency from point generation to anomaly evaluation.