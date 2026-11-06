# Source Diff: 0.1.3 vs 0.2.3 (`src/test`)

> Scope: `src/test` only. `src/main` is covered in [main-diff-0.1.3-vs-0.2.3.md](main-diff-0.1.3-vs-0.2.3.md).
> Excluded from the comparison: Java 8 vs Java 17 syntax/API differences, and Java EE vs Jakarta EE API differences. Neither category produced any change in this pair of tags.

## 1. Module matrix

| Maven module | `src/test` files 0.1.3 | `src/test` files 0.2.3 | Changed files | Verdict |
|---|---|---|---|---|
| microsphere-observability-logging | 4 | 4 | 0 | identical |
| microsphere-observability-metrics-commons | 0 | 0 | 0 | no tests |
| microsphere-observability-metrics-alibaba-sentinel | 2 | 2 | 0 | identical |
| microsphere-observability-metrics-micrometer | 20 | 20 | 0 | identical |
| microsphere-observability-metrics-prometheus | 3 | 3 | 0 | identical |
| microsphere-observability-logging-spring-boot | 9 | 9 | 0 | identical |
| microsphere-observability-metrics-spring-boot | 13 | 13 | 2 | **differs** |
| microsphere-observability-metrics-spring-cloud | 2 | 2 | 1 (rename + edit) | **differs** |

Test coverage is otherwise byte-identical between the two lines; only three test classes differ, all of them integration tests that bind to Spring Boot / Micrometer / Prometheus coordinates.

## 2. microsphere-observability-metrics-spring-boot

| File | Change in 0.2.3 | Reason category |
|---|---|---|
| `…/autoconfigure/JvmMetricsAutoConfigurationIntegrationTest.java` | Expected meter count for the `test`-prefixed executor-service binders raised from `18` to `20` | Functional expectation — the newer Micrometer release used by the 0.2.x line registers two additional `ExecutorServiceMetrics` timers/gauges per cached thread pool; the assertion is a count of the binder output, not a code change |
| `…/autoconfigure/SentinelMetricsAutoConfigurationIntegrationTest.java` | Import `io.micrometer.prometheus.PrometheusMeterRegistry` → `io.micrometer.prometheusmetrics.PrometheusMeterRegistry`; `@SpringBootTest(properties=…)` key `management.metrics.export.prometheus.enabled=true` → `management.prometheus.metrics.export.enabled=true` | API compatibility — follows the main-source migration of both the Micrometer Prometheus package and the Spring Boot Prometheus export property namespace; the old property is ignored by the new stack, so the test would not activate the export without it |

## 3. microsphere-observability-metrics-spring-cloud

| File | Change in 0.2.3 | Reason category |
|---|---|---|
| `ServiceRegistrationMetricsAutoConfigurationIntegrationTest.java` → `ServiceRegistrationMetricsAutoConfigurationIT.java` (92% similarity rename) | Class renamed and `@SpringBootTest(classes=…)` self-reference updated; property set now keeps the legacy `management.metrics.export.prometheus.enabled=true` **and** adds `management.prometheus.metrics.export.enabled=true` | Naming convention + API compatibility — the `IT` suffix matches the failsafe/integration-test convention adopted on the 0.2.x line; both property keys are kept so the same test passes against either Spring Boot generation, which mirrors the additive `@AutoConfigureAfter` strategy in `src/main` |

## 4. Observations

1. The `metrics-spring-cloud` test is the only place where the old and new Prometheus property keys coexist; the `metrics-spring-boot` test replaced the key outright. That asymmetry is deliberate: the IT in the Spring Cloud module is intended to be portable across both generations, while the Boot-module integration test only ever runs on the 0.2.x line.
2. `microsphere-observability-metrics-prometheus` tests (`SentinelMultiCollectorTest`, `SentinelCollectorTest`, `SentinelMetricsTestHelper`) are identical in both tags, confirming that the collector switch observed in `src/main` was already fully supported by tests on both lines.
3. No test was added or removed overall (13/13 and 2/2 file counts hold), so the diff is limited to coordinate and expectation updates.
