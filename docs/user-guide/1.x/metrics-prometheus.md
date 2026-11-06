# metrics-prometheus — User Guide (1.x line)

[中文版](./zh/metrics-prometheus.md) · [User Guide index](./README.md)

## Overview

`microsphere-observability-metrics-prometheus` renders the Alibaba Sentinel metric catalogue owned by
`metrics-alibaba-sentinel` into the two Prometheus client APIs still in circulation. It ships exactly two public types,
one per client API:

- `SentinelMultiCollector` — implements `io.prometheus.metrics.model.registry.MultiCollector` (client_java 1.x,
  artifact `io.prometheus:prometheus-metrics-core`), returns `MetricSnapshots`.
- `SentinelCollector` — extends the legacy `io.prometheus.client.Collector` (artifact `io.prometheus:simpleclient`),
  returns `List<Collector.MetricFamilySamples>`.

Both read the same 7 `GAUGE` families (`sentinel_rt`, `sentinel_concurrency`, `sentinel_success_qps`,
`sentinel_pass_qps`, `sentinel_occupied_pass_qps`, `sentinel_block_qps`, `sentinel_exception_qps`) and the same 4
required labels (`sentinel_resource`, `sentinel_context`, `sentinel_resource_type`, `sentinel_version`); both take a
collection window in milliseconds, both offer a fluent `commonLabel(name, value)`, and both degrade to "nothing
exported" (empty result plus a warning log) when the Sentinel metrics repository is not installed, so a scrape never
fails. Pick one per JVM. The module itself has no registry, no exposition server, no push gateway and no Spring code:
you hand an instance to `PrometheusRegistry` (new client) or `CollectorRegistry` (legacy client), or let
[metrics-spring-boot](./metrics-spring-boot.md) do it.

On this branch the module's Java sources and tests are **byte-identical to `release`**:
`git diff origin/release origin/release-1.x -- microsphere-observability-metrics-prometheus` lists only `pom.xml`
(7 added lines, an optional `micrometer-core`). What differs is the surrounding world — Prometheus client BOM `1.8.0`,
and `metrics-spring-boot` here auto-configuring the **legacy** `SentinelCollector`. This document stands alone; the
cross-line summary is the last section.

## Version and compatibility

| Attribute | Value |
|:---|:---|
| Branch analysed | `release-1.x` |
| Maven coordinates | `io.github.microsphere-projects:microsphere-observability-metrics-prometheus` |
| Version line | `0.1.x` (reactor `${revision}` = `0.1.4-SNAPSHOT`, latest released tag `0.1.3`) |
| Java version | not declared by the 1.x reactor pom (inherited from `microsphere-spring-cloud-parent:0.1.26`) |
| `@since` of both collectors | `1.0.0` |
| Prometheus client BOM | `io.prometheus:prometheus-metrics-bom` `1.8.0` (`${prometheus.version}` in `microsphere-observability-parent`) |
| Module API delta vs `current` | none — only the pom and the downstream wiring differ |
| Javadoc | [io.github.microsphere-projects/microsphere-observability-metrics-prometheus](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-prometheus) |

| Dependency | Scope | Needed at runtime for |
|:---|:---|:---|
| `microsphere-observability-metrics-commons` | compile | the backend-neutral `MetricFamily` model |
| `microsphere-observability-metrics-alibaba-sentinel` | compile | `SentinelMetricUtitls` — catalogue, labels, value readers |
| `io.prometheus:prometheus-metrics-core` | optional | `SentinelMultiCollector` only |
| `io.prometheus:simpleclient` | optional | `SentinelCollector` only |
| `io.micrometer:micrometer-core` | optional | nothing here — no source file in this module imports `io.micrometer`; declared on this line only |
| `com.alibaba.csp:sentinel-core` | optional | `MetricNode` — the source of every value and label |
| `io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` | optional | `SentinelMetricsRepository` behind `getContextMetricNodesMap(interval)` |
| `microsphere-java-core`, `microsphere-annotation-processor` | optional | logging/collection helpers, build-time processing |
| `junit-jupiter`, `prometheus-metrics-instrumentation-jvm`, `prometheus-metrics-exporter-httpserver`, `microsphere-java-test`, `logback-classic` | test | module tests |

Both Prometheus artifacts are **optional**, so instantiating a collector whose client API is missing ends in
`NoClassDefFoundError`. `prometheus-metrics-core` resolves to `1.8.0` from the imported BOM; `simpleclient` carries no
version in the module pom and comes from the `dependencyManagement` inherited by `microsphere-observability-parent` —
`./mvnw -pl microsphere-observability-metrics-prometheus dependency:tree` prints the effective number. The repository
README puts the `1.x` line on Spring Cloud `Hoxton – 2021.0.x`; Spring generations are chosen by the reactor parent
`microsphere-spring-cloud-parent:0.1.26` through profiles (default in bold):

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|:---|:---|:---|:---|
| `spring-cloud-hoxton` | 2.3.12.RELEASE | Hoxton.SR12 | 2.2.10 |
| `spring-cloud-2020` | 2.4.13 | 2020.0.5 | 2021.1 |
| **`spring-cloud-2021` (default)** | **2.6.15** | **2021.0.9** | **2021.0.6.1** |

## Installation

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-observability-metrics-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>io.prometheus</groupId>
    <artifactId>simpleclient</artifactId>
</dependency>
```

`simpleclient` is what Boot 2.6 + `micrometer-registry-prometheus` already bring, and it is what this line's
auto-configuration uses with `SentinelCollector`. Add `com.alibaba.csp:sentinel-core` and
`io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` too: optional in this module, but without them no
data point is ever produced. To use the new client instead, also declare `io.prometheus:prometheus-metrics-core`
(here `1.8.0`, managed by `io.prometheus:prometheus-metrics-bom`). Observability artifact versions are managed by the
consumer BOM [microsphere-observability-dependencies](./dependencies.md) (`0.1.3` for released builds).

## Quick start

Legacy client (`simpleclient`) — the collector the auto-configuration on this line uses:

```java
import io.microsphere.metrics.prometheus.sentinel.client.SentinelCollector;
import io.prometheus.client.CollectorRegistry;

SentinelCollector collector = new SentinelCollector(60_000L).commonLabel("application", "demo");
collector.register(new CollectorRegistry());    // the collector registers itself
```

New client (`MultiCollector`) — available on this line too, but you add `prometheus-metrics-core` yourself:

```java
import io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

SentinelMultiCollector collector = new SentinelMultiCollector(60_000L).commonLabel("application", "demo");
new PrometheusRegistry().register(collector);   // a MultiCollector is registered by the registry
```

Each scrape calls `SentinelMetricUtitls.getContextMetricNodesMap(interval)` and renders what Sentinel recorded there.

## API reference

Identical on both version lines; restated so this document stands alone.

### `io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector`

`public class SentinelMultiCollector implements io.prometheus.metrics.model.registry.MultiCollector`

| Signature | Description |
|:---|:---|
| `SentinelMultiCollector(long interval)` | `interval` is the collection window in **milliseconds**; the 7 `MetricFamilyDescriptor`s are built eagerly in this constructor |
| `SentinelMultiCollector commonLabel(String name, String value)` | Adds one constant label to every data point, returns `this`; a repeated `name` replaces its value |
| `MetricSnapshots collect()` | The `MultiCollector` contract. Walks `Map<context, List<MetricNode>>` over `[now - interval, now]`, returns one `GaugeSnapshot` per family, or an empty `MetricSnapshots` plus a warning log when nothing was recorded |
| `List<MetricFamilyDescriptor> getMetricFamilyDescriptors()` | The `MultiCollector` contract. The descriptors built at construction time, used to advertise `# HELP` / `# TYPE` |
| `Labels toLabels(String context, MetricNode metricNode)` | Public helper: the 4 required labels + configured common labels + `sentinel_timestamp`, rendered as `io.prometheus.metrics.model.snapshots.Labels` |

No method declares `throws`; the only failure modes are the `NullPointerException` / `IndexOutOfBoundsException`
contracts of `SentinelMetricUtitls`, unreachable for the fixed index range `0..6` used here. Not synchronised — finish
all `commonLabel(...)` calls before registering.

### `io.microsphere.metrics.prometheus.sentinel.client.SentinelCollector`

`public class SentinelCollector extends io.prometheus.client.Collector`

| Signature | Description |
|:---|:---|
| `SentinelCollector(long interval)` | Collection window in **milliseconds**; nothing is built up front — family metadata is read from the catalogue on every scrape |
| `SentinelCollector commonLabel(String name, String value)` | Adds one constant label to every sample, returns `this` |
| `List<MetricFamilySamples> collect()` | The `Collector` contract. Returns 7 `MetricFamilySamples` whose names come from `SentinelMetricUtitls.buildMetricName(name, unit)` (so `sentinel_rt` is exported as `sentinel_rt_millseconds`), or `Collections.emptyList()` when the repository yields nothing |
| `register()` / `register(CollectorRegistry)` | Inherited from `io.prometheus.client.Collector`; the no-argument form installs into `CollectorRegistry.defaultRegistry` |

Label lists come from `combineLabels(context, metricNode, commonLabels)` per sample, and the node timestamp travels as
the `Sample` timestamp (`new Sample(metricName, labelNames, labelValues, value, timestampMs)`) **rather than** a
`sentinel_timestamp` label. Because auto-configuration on this line uses the legacy collector, the series you get from
`/actuator/prometheus` carry no `sentinel_timestamp` label.

## Configuration

The module reads no properties and declares no configuration key; callers tune one constructor argument and label
pairs.

| Knob | Where | Value used in this repo's own tests | Effect |
|:---|:---|:---|:---|
| `interval` | constructor argument, milliseconds | `60000` | The `[now - interval, now]` window passed to `SentinelMetricUtitls.getContextMetricNodesMap(long)` |
| common labels | `commonLabel(String, String)` | `test-label=test-value`, `application=test` | Appended to the 4 required labels on every data point; required labels **win** on a name collision because `combineLabels` inserts common labels first |
| registry | your `CollectorRegistry` or `PrometheusRegistry` | a fresh instance per test | Decides the exposition endpoint; the collectors are registry-agnostic |

### Wiring through `metrics-spring-boot`

`SentinelMetricsAutoConfiguration` (package `io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure`)
registers the **legacy** collector here. Its nested `PrometheusConfiguration` is gated by
`@ConditionalOnEnabledPrometheusMetricsExport`, itself `@ConditionalOnEnabledMetricsExport("prometheus")` plus
`@ConditionalOnBean(type = "io.micrometer.prometheus.PrometheusMeterRegistry")` — the Boot 2-era registry type. The bean
method `sentinelCollector(PrometheusMeterRegistry registry, MetricsProperties properties, @Value(METRICS_COLLECTION_INTERVAL_PLACEHOLDER) Duration interval)`
carries one extra guard and reduces to:

```java
@ConditionalOnClass(name = {"io.prometheus.client.Collector"})
SentinelCollector sentinelCollector = new SentinelCollector(interval.toMillis());
Map<String, String> commonTags = properties.getTags();      // Common tags covert to Prometheus labels
commonTags.forEach(sentinelCollector::commonLabel);
CollectorRegistry prometheusRegistry = registry.getPrometheusRegistry();
sentinelCollector.register(prometheusRegistry);
```

| Property | Default | Role |
|:---|:---|:---|
| `microsphere.metrics.micrometer.alibaba-sentinel.enabled` | `true` (`matchIfMissing = true`) | switches the whole Sentinel metrics path, this collector included |
| `management.metrics.tags.*` | `application`, `profiles` in the shipped `META-INF/config/default/metrics.yaml` | read through `MetricsProperties.getTags()`, then applied as `commonLabel`s |
| `system.metrics.collection.interval` | `60000`; `metrics.yaml` maps it to `${management.metrics.export.prometheus.pushgateway.push-rate:60000}` | the `Duration` handed to `interval.toMillis()` |

The Micrometer binder bean `SentinelMetrics` is created only
`@ConditionalOnMissingBean(type = "io.microsphere.metrics.prometheus.sentinel.client.SentinelCollector")`, so your own
`SentinelCollector` bean also suppresses the Micrometer path — but a `SentinelMultiCollector` bean does **not**, unlike
on the `current` line. Registration of the auto-configuration happens through both
`META-INF/spring/…AutoConfiguration.imports` and the legacy `META-INF/spring.factories` on this line.

## Code examples

The module's tests are byte-identical to `release`. From
`src/test/java/io/microsphere/metrics/prometheus/sentinel/client/SentinelCollectorTest.java`, whose `setUp` uses
`new SentinelCollector(60000).commonLabel("test-label", "test-value")` and `sentinelCollector.register(registry)` on a
fresh `CollectorRegistry`:

```java
@Test
void testCollect() throws Throwable {
    this.testHelper.doInSentinelMetrics(() -> {
        Enumeration<MetricFamilySamples> metricFamilySamples = this.registry.metricFamilySamples();
        List<MetricFamilySamples> metricFamilySamplesList = newArrayList(metricFamilySamples);
        assertMetricFamilySamplesList(metricFamilySamplesList);   // asserts size == 7, then name/type/help per index
    });
}

@Test
void testCollectOnSentinelMetricsRepositoryNotReady() {
    List<MetricFamilySamples> metricFamilySamplesList = newArrayList(this.registry.metricFamilySamples());
    assertTrue(metricFamilySamplesList.isEmpty());
}
```

The per-index assertions are `samples.name == buildMetricName(metricFamily)`,
`samples.type.name() == metricFamily.getType().name()` and `samples.help == metricFamily.getHelp()`. The new-client test
in `…/sentinel/SentinelMultiCollectorTest.java` is set up with
`new SentinelMultiCollector(60000).commonLabel("application", "test")` registered on a `PrometheusRegistry`, and asserts
`assertEquals(7, metricSnapshots.size())` after a scrape and `assertEquals(0, …size())` with no repository installed.

Both use `SentinelMetricsTestHelper` (same test package): it creates a `SentinelMetricsRepository`, installs it with
`addEntryCallback(...)`, drives `100` calls of `new SentinelTemplate().call("test-resource", () -> sleep(10))`, spins
with `sleep(100)` until `findMetricNodes(beginTimeMs, currentTimeMillis())` is non-empty, runs the scrape action, and
always calls `removeEntryCallback(sentinelMetricsRepository.getClass())` in a `finally`. It starts no HTTP server and
needs no external service.

## Metrics reference

Catalogue source: `SentinelMetricUtitls.METRIC_FAMILIES` (`metrics-alibaba-sentinel`); all families are `GAUGE` and
carry the 4 required labels plus your common labels.

| Metric (authored) | Unit | Type | HELP text | Legacy export name |
|:---|:---|:---|:---|:---|
| `sentinel_rt` | `millseconds` | `GAUGE` | The Response Time (RT) of the Sentinel Resource | `sentinel_rt_millseconds` |
| `sentinel_concurrency` | — | `GAUGE` | The Concurrency of the Sentinel Resource | `sentinel_concurrency` |
| `sentinel_success_qps` | — | `GAUGE` | The Success QPS of the Sentinel Resource | `sentinel_success_qps` |
| `sentinel_pass_qps` | — | `GAUGE` | The Pass QPS of the Sentinel Resource | `sentinel_pass_qps` |
| `sentinel_occupied_pass_qps` | — | `GAUGE` | The Occupied Pass QPS of the Sentinel Resource | `sentinel_occupied_pass_qps` |
| `sentinel_block_qps` | — | `GAUGE` | The Block QPS of the Sentinel Resource | `sentinel_block_qps` |
| `sentinel_exception_qps` | — | `GAUGE` | The Block QPS of the Sentinel Resource | `sentinel_exception_qps` |

Unlike the legacy path, `SentinelMultiCollector` never concatenates the unit itself: it names descriptors through
`PrometheusNaming.sanitizeMetricName(name)`, or `sanitizeMetricName(name, unit)` for the single family carrying a unit,
and hands that `Unit` to the descriptor builder. Labels on every data point:

| Label | Value | Emitted by |
|:---|:---|:---|
| `sentinel_resource` | `MetricNode.getResource()` | both |
| `sentinel_context` | the context key of the repository map | both |
| `sentinel_resource_type` | `SentinelUtils.getResourceTypeAsString(classification)` | both |
| `sentinel_version` | `com.alibaba.csp.sentinel.Constants.SENTINEL_VERSION` | both |
| your `commonLabel(...)` pairs | constant | both |
| `sentinel_timestamp` | `MetricNode.getTimestamp()` as a decimal string | `SentinelMultiCollector` only; the legacy collector puts the same value in the `Sample` timestamp |

## Notes and limitations

- `metrics-spring-boot` and `metrics-spring-cloud` on this line work against `io.prometheus:simpleclient`, so
  auto-configured series look like the legacy column above; the new client works only if you add its artifact and
  register the collector yourself.
- Register **one** collector per JVM: both read the same repository and emit the same 7 families in different models.
- `SentinelMultiCollector` freezes its descriptors in the constructor, so common labels added afterwards appear on the
  samples but not in the descriptor's declared `labelNames` — configure labels first, register second.
- An empty scrape is logged, not thrown: `No metrics collected` from either collector, and
  `SentinelMetricsRepository is not ready, please check whether it is configured` from the catalogue utility.
- Two catalogue artefacts reach the exposition: `sentinel_exception_qps` carries the help text
  `"The Block QPS of the Sentinel Resource"` (upstream copy error) and the `sentinel_rt` unit is spelled `millseconds`,
  which the legacy collector also reports twice (name plus `MetricFamilySamples` unit).
- Values are per-`MetricNode` instants (Sentinel writes one node per second per resource), not aggregates over
  `interval`, so a window covering several seconds yields several data points per series.
- The module ships no `AutoConfiguration.imports`, `spring.factories` or service file of its own.

## Version differences vs the `current` line

| Item | `0.1.x` (`release-1.x`) | `0.2.x` (`release`) |
|:---|:---|:---|
| Module Java sources and tests | — | identical |
| Module pom | adds optional `io.micrometer:micrometer-core` | no `micrometer-core` |
| Prometheus client BOM | `1.8.0` | `1.9.0` |
| Collector registered by `metrics-spring-boot` | `SentinelCollector` (legacy `simpleclient`), tags via `MetricsProperties.getTags()` | `SentinelMultiCollector` (new client), tags via `getSubProperties(environment, "management.metrics.tags")` |
| Registry object used there | `registry.getPrometheusRegistry()` → `io.prometheus.client.CollectorRegistry` | `registry.getPrometheusRegistry()` → `io.prometheus.metrics.model.registry.PrometheusRegistry` |
| Meter registry type required by the gate | `io.micrometer.prometheus.PrometheusMeterRegistry` | `io.micrometer.prometheusmetrics.PrometheusMeterRegistry` |
| `sentinel_timestamp` label in auto-configured output | absent (sample timestamp instead) | present |
| Micrometer binder suppression key | `…sentinel.client.SentinelCollector` | `…sentinel.SentinelMultiCollector` |
| Auto-config registration downstream | `.imports` + `spring.factories` | `.imports` only |
| Default Spring Boot / Spring Cloud | 2.6.15 / 2021.0.9 | 3.5.15 / 2025.0.3 |

## Related documents

- [中文用户文档](./zh/metrics-prometheus.md)
- [Developer Guide — metrics-prometheus (1.x)](../../developer-guide/1.x/metrics-prometheus.md)
- [User Guide — current / release branch](../current/metrics-prometheus.md)
- [User Guide index for this branch](./README.md)
