# Metrics Micrometer Binders — User Guide

[中文版](./zh/metrics-micrometer.md) · [User Guide index](../README.md)

## Overview

`microsphere-observability-metrics-micrometer` ships custom Micrometer `MeterBinder` implementations plus one P6Spy
`JdbcEventListener` for metrics Micrometer's own JVM binders do not expose. It is plain Java: no Spring dependency, no
auto-configuration class, nothing self-registers — you call `bindTo(MeterRegistry)`, or let
[`microsphere-observability-metrics-spring-boot`](../metrics-spring-boot.md) call it for you.

- `AbstractMeterBinder`: template-method base adding an `origin` tag, a `supports()` gate, exception swallowing.
- `MBeanMetrics` + `MBeanAttributeMeterBinder`: bind any MBean attributes you select.
- `SystemMemoryMetrics`: five swap / physical / virtual memory gauges from the platform `OperatingSystemMXBean`.
- `CGroupMemoryMetrics`: container memory gauges read from `/sys/fs/cgroup/memory/`.
- `NetworkStatisticsMetrics`: rx/tx bytes, packets, errors, drops per interface from `/proc/net/dev`, on a schedule.
- `SentinelMetrics`: seven Sentinel `ClusterNode` gauges per resource, driven by `ClusterNodeAddedEvent`.
- `MicrometerJdbcEventListener`: SQL success/failure counters plus a slow-SQL gauge, discovered via `META-INF/services`.
- `SystemConstants` / `SystemUtils` / `MicrometerUtils`: configuration keys, file readers, the shared async executor.

## Version and compatibility

| Fact | Value |
|---|---|
| Branch | `release` |
| Reactor `${revision}` / latest released version | `0.2.4-SNAPSHOT` / `0.2.3` |
| Root pom parent | `microsphere-spring-cloud-parent:0.2.26` |
| `java.version` declared by the reactor | `17` |
| `microsphere-spring-cloud` / `microsphere-alibaba-sentinel` / `microsphere-i18n` BOMs | `0.2.26` / `0.2.5` / `0.2.21` |
| `prometheus.version` (`io.prometheus:prometheus-metrics-bom`) | `1.9.0` |
| `opentelemetry-instrumentation-bom` | `2.31.1` |
| `io.micrometer:micrometer-core`, `p6spy:p6spy`, `com.alibaba.csp:sentinel-core` | not pinned here; managed by the upstream BOM chain |

Spring is not pinned here either: it comes from `microsphere-spring-cloud-parent` profiles selected with
`-P spring-cloud-2022` and friends, so the module builds against the selected profile, default in bold. The repository
README states the 0.2.x line targets Spring Cloud 2022.0.x–2025.0.x.

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|---|---|---|---|
| `spring-cloud-2022` | 3.0.13 | 2022.0.5 | 2022.0.0.2 |
| `spring-cloud-2023` | 3.3.13 | 2023.0.6 | 2023.0.3.4 |
| `spring-cloud-2024` | 3.4.13 | 2024.0.3 | 2023.0.3.4 |
| **`spring-cloud-2025` (default)** | **3.5.15** | **2025.0.3** | **2025.0.0.0** |
| `spring-cloud-2025.1` | 4.0.8 | 2025.1.3 | 2025.1.0.0 |

The module `pom.xml` is identical on `release` and `release-1.x`; the 0.1.x/0.2.x delta is the version line and the
managed Micrometer / Prometheus versions, not the API described below.

## Installation

`micrometer-core`, `micrometer-registry-prometheus`, `p6spy`, `microsphere-java-core`, the Sentinel artifacts and
`microsphere-annotation-processor` are `<optional>` in this pom and therefore **not** transitive — declare what you use.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.microsphere-projects</groupId>
      <artifactId>microsphere-observability-dependencies</artifactId>
      <version>0.2.3</version><type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency><groupId>io.github.microsphere-projects</groupId><artifactId>microsphere-observability-metrics-micrometer</artifactId></dependency>
  <dependency><groupId>io.micrometer</groupId><artifactId>micrometer-core</artifactId></dependency>
  <dependency><groupId>io.github.microsphere-projects</groupId><artifactId>microsphere-java-core</artifactId></dependency>
  <dependency><groupId>p6spy</groupId><artifactId>p6spy</artifactId></dependency>
</dependencies>
```

## Quick start

```java
MeterRegistry registry = new SimpleMeterRegistry();
new SystemMemoryMetrics().bindTo(registry);      // system.memory.*
new CGroupMemoryMetrics().bindTo(registry);      // system.cgroup.*, /sys/fs/cgroup/memory/**
new NetworkStatisticsMetrics().bindTo(registry); // system.network.*, /proc/net/dev every 60s
```

`bindTo` evaluates `supports()` first: `false` logs `Current Metrics is not supported` and registers nothing, and a
`Throwable` from `doBindTo()` is logged as `Bind To MeterRegistry[{}] failed` and swallowed — a misconfigured binder
shows up as missing metrics, never as a startup failure.

## API reference

### `AbstractMeterBinder` (`io.microsphere.metrics.micrometer.instrument.binder`)

```java
public AbstractMeterBinder() / public AbstractMeterBinder(Iterable<Tag> commonTags)
public final void bindTo(MeterRegistry registry)
protected abstract boolean supports(MeterRegistry registry)
protected abstract void doBindTo(MeterRegistry registry) throws Throwable
protected String getOriginTagValue() / protected Iterable<Tag> combine(String... tags)
public static final String ORIGIN_TAG_KEY = "origin";
```

`commonTags` is the constructor tags plus `origin=getSimpleName(getClass())` of the concrete binder, so
`CGroupMemoryMetrics` yields `origin=CGroupMemoryMetrics` and subclasses change it automatically; `combine` appends more
key/value pairs to one meter. No synchronisation is added — `bindTo` is meant to run once per registry — and `MBeanMetrics`
is the one binder that does not apply `commonTags` at all.

### JMX: `MBeanMetrics` and `MBeanAttributeMeterBinder` (`.binder.jmx`)

`MBeanAttributeMeterBinder` is the per-attribute callback, and the place metric names get invented, because the module
provides **no** attribute-to-metric naming rule:

```java
void bindTo(ObjectName mBeanName, MBeanInfo mBeanInfo, MBeanAttributeInfo mBeanAttributeInfo,
            Object attributeValue, MeterRegistry registry) throws Throwable;
// MBeanMetrics constructors (1 = platform server, 2 = nullable server / no QueryExp, 3 = full form):
public MBeanMetrics(@Nonnull ObjectName objectNameToQuery, @Nonnull MBeanAttributeMeterBinder... binders)
public MBeanMetrics(@Nullable MBeanServer server, @Nonnull ObjectName objectNameToQuery, @Nonnull MBeanAttributeMeterBinder... binders)
public MBeanMetrics(@Nonnull MBeanServer server, @Nonnull ObjectName objectNameToQuery, @Nullable QueryExp queryExp, @Nonnull MBeanAttributeMeterBinder... binders)
```

`supports()` is `attributeMeterBinders.length > 0`. `doBindTo()` runs `mBeanServer.queryNames(objectNameToQuery, queryExp)`,
reads **all** attributes of every matched `ObjectName` through `JmxUtils.getMBeanAttributes(mBeanServer, objectName)` and
hands each (`mBeanName`, `MBeanInfo`, `MBeanAttributeInfo`, value) tuple to every binder. So which MBeans are bound is
purely your `ObjectName` (the test binds `java.util.logging:type=Logging` and its `LoggerNames` attribute), attributes are
sampled once at bind time with no rescan and no refresh, and an empty `queryNames` result (MBean absent, wrong domain,
`QueryExp` mismatch) registers nothing.

### `SystemMemoryMetrics` (`.binder.system`)

```java
public SystemMemoryMetrics() / public SystemMemoryMetrics(Iterable<Tag> tags)
public static boolean isSupported() / public static final boolean SUPPORTED
public static final String OPERATING_SYSTEM_MXBEAN_CLASS_NAME = "com.sun.management.OperatingSystemMXBean";
public static final String METRIC_PREFIX = SystemConstants.PREFIX;   // "system."
```

`SUPPORTED` is computed at class load: the class is resolved with the system class loader and looked up with
`ManagementFactory.getPlatformMXBean(...)` inside `ThrowableSupplier.execute(..., e -> false)`, so any throwable yields
`false` and nothing binds. The gate is that lookup, not an OS-name check — Windows, macOS and Linux all expose the
HotSpot `com.sun.management.OperatingSystemMXBean`, so the gauges bind wherever the class resolves; values the OS does
not provide (swap on macOS) come back as `-1`. Suppliers use the long-returning accessors
(`getTotalSwapSpaceSize`, `getFreeSwapSpaceSize`, `getCommittedVirtualMemorySize`, `getTotalPhysicalMemorySize`,
`getFreePhysicalMemorySize`) on `JmxUtils.getOperatingSystemMXBean()`, not the newer
`getTotalMemorySize()` / `getFreeMemorySize()` pair.

| Metric | Type | Description | Base unit | Tags |
|---|---|---|---|---|
| `system.memory.swap.space.total` | Gauge | Total swap space size | `bytes` | `origin` |
| `system.memory.swap.space.free` | Gauge | Free swap space size | `bytes` | `origin` |
| `system.memory.committed.virtual` | Gauge | Committed virtual memory size | `bytes` | `origin` |
| `system.memory.physical.total` | Gauge | Total physical memory size | `bytes` | `origin` |
| `system.memory.physical.free` | Gauge | Free physical memory size | `bytes` | `origin` |

### `CGroupMemoryMetrics` (`.binder.system`)

```java
public CGroupMemoryMetrics()   // SystemUtils.getCGroupDirectoryPath()
public CGroupMemoryMetrics(Path cgroupDirectoryPath)
public CGroupMemoryMetrics(Path cgroupDirectoryPath, Iterable<Tag> tags)
public static final String METRIC_PREFIX = SystemConstants.CGROUP_PREFIX;   // "system.cgroup."
```

`"memory"` is appended to the directory, so the default scan path is `/sys/fs/cgroup/memory/` and `memory/stat` resolves
inside it. `supports()` is `Files.exists(memoryDirectoryPath)`, logging
`The CGroup memory directory[path: '{}'] does not exist!` otherwise: on Windows, on macOS and on Linux without the
cGroup v1 memory controller (cgroup v2 hierarchies included) nothing is registered.

| Metric | Type | Source | Description | Base unit | Tags |
|---|---|---|---|---|---|
| `system.cgroup.memory.usage_in_bytes` | Gauge | first line of `memory.usage_in_bytes` | current memory usage | `bytes` | `origin` |
| `system.cgroup.memory.max_usage_in_bytes` | Gauge | `memory.max_usage_in_bytes` | peak memory usage | `bytes` | `origin` |
| `system.cgroup.memory.memsw.usage_in_bytes` | Gauge | `memory.memsw.usage_in_bytes` | current memory + swap | `bytes` | `origin` |
| `system.cgroup.memory.memsw.max_usage_in_bytes` | Gauge | `memory.memsw.max_usage_in_bytes` | peak memory + swap | `bytes` | `origin` |
| `system.cgroup.memory.limit_in_bytes` | Gauge | `memory.limit_in_bytes` | configured limit | `bytes` | `origin` |
| `system.cgroup.memory.stat.<statistic>` | Gauge | one key/value pair of `memory.stat` | per-statistic memory | `bytes` | `origin` |

The five file gauges re-read their file at every collection (`readFileAsLong`). `memory.stat` is parsed once at bind
time (split on the first space, space-less lines skipped) and eleven statistics become gauges: `cache`, `rss`,
`mapped_file`, `swap`, `active_anon`, `inactive_anon`, `active_file`, `inactive_file`, `unevictable`,
`hierarchical_memory_limit`, `hierarchical_memsw_limit`. A non-numeric value is skipped with
`memory.stat Statistics : {} is not numeric`; a statistic missing from the file registers nothing.

### `NetworkStatisticsMetrics` (`.binder.system`)

```java
public NetworkStatisticsMetrics()   // get(getNetworkStatsFilePath()), getMetricsCollectionInterval()
public NetworkStatisticsMetrics(Path statsFilePath, long interval)
public NetworkStatisticsMetrics(Path statsFilePath, long interval, @Nullable ScheduledExecutorService executor)
public NetworkStatisticsMetrics(Path statsFilePath, long interval, @Nullable ScheduledExecutorService executor,
                                @Nullable Iterable<Tag> tags)
public static final String METRIC_PREFIX = SystemConstants.NETWORK_PREFIX;   // "system.network."
```

`supports()` is `Files.exists(statsFilePath)`; `/proc/net/dev` does not exist on Windows or macOS, so nothing registers
there. `doBindTo()` parses the file once immediately, then schedules `bindStats` at a fixed rate of `interval` ms on the
given executor, defaulting to `MicrometerUtils.getScheduledExecutor()`. Parsing starts at line index 2 (past the two
`Inter-|   Receive |  Transmit` header lines) and decodes each `name: numbers` row positionally: tokens 0-3 are receive
bytes/packets/errors/drops, token 4 is skipped, tokens 8-11 are the transmit quartet; unparseable rows log
`The Stats can't be parsed by the line {} : {}`. Each device's eight gauges are registered once (`computeIfAbsent`) with
tag `interface=<device name>`; later reads only update the `Stats` holder inside `synchronized (this)`.

| Metric | Type | Description | Base unit | Tags |
|---|---|---|---|---|
| `system.network.receive.bytes` | Gauge | Number of good received bytes | `bytes` | `origin`, `interface` |
| `system.network.receive.packets` | Gauge, strong reference | Number of good packets received by the interface | — | `origin`, `interface` |
| `system.network.receive.errors` | Gauge, strong reference | Total number of bad packets received on this network device | — | `origin`, `interface` |
| `system.network.receive.drop` | Gauge, strong reference | Number of packets received but not processed | — | `origin`, `interface` |
| `system.network.transmit.bytes` | Gauge | Number of good transmitted bytes | `bytes` | `origin`, `interface` |
| `system.network.transmit.packets` | Gauge, strong reference | Number of packets successfully transmitted | — | `origin`, `interface` |
| `system.network.transmit.errors` | Gauge, strong reference | Total number of transmit problems | — | `origin`, `interface` |
| `system.network.transmit.drop` | Gauge, strong reference | Number of packets dropped on their way to transmission | — | `origin`, `interface` |

### `SentinelMetrics` (`.binder.sentinel`)

```java
public SentinelMetrics() / public SentinelMetrics(Iterable<Tag> tags)
public void onEvent(ClusterNodeAddedEvent event)
public static final String RESOURCE_TAG_KEY = "sentinel_resource";   // CONTEXT/TYPE/VERSION_TAG_KEY likewise
```

`SentinelMetrics extends AbstractMeterBinder implements ClusterNodeAddedEventListener`; the constructor appends
`sentinel_version=<Constants.SENTINEL_VERSION>` to `commonTags`. `supports()` requires
`SentinelNodeEventPublisher.getSentinelNodeEventPublisher() != null`, which only holds once the publisher was installed in
Sentinel's entry-callback chain (`ProcessorSlotCallbackUtils.addEntryCallback(new SentinelNodeEventPublisher())` in the
test; a `META-INF/services/io.microsphere.alibaba.sentinel.common.callback.DefaultNodeEntryCallback` entry in the
spring-boot module). Without it `bindTo()` registers nothing and the package-private `registry` field stays `null`;
`doBindTo()` stores the registry, subscribes `this`, and meters then appear lazily, one set per event.

| Metric | Type | Supplier | Base unit | Tags besides `origin`, `sentinel_version` |
|---|---|---|---|---|
| `sentinel_rt` | TimeGauge | `ClusterNode::avgRt`, unit `MILLISECONDS` | derived by Micrometer from the unit | `sentinel_resource`, `sentinel_context`, `sentinel_resource_type` |
| `sentinel_concurrency` | Gauge | `ClusterNode::totalRequest` | — | same |
| `sentinel_success_qps` | Gauge | `ClusterNode::successQps` | — | same |
| `sentinel_pass_qps` | Gauge | `ClusterNode::passQps` | — | same |
| `sentinel_occupied_pass_qps` | Gauge | `ClusterNode::occupiedPassQps` | — | same |
| `sentinel_block_qps` | Gauge | `ClusterNode::blockQps` | — | same |
| `sentinel_exception_qps` | Gauge | `ClusterNode::exceptionQps` | — | same |

`sentinel_resource_type` is `SentinelUtils.getResourceTypeAsString(clusterNode.getResourceType())`; metric names and tag
keys come from `SentinelMetricsConstants` (prefix `sentinel_`) in `microsphere-observability-metrics-alibaba-sentinel`.

### `SystemConstants`, `SystemUtils`, `MicrometerUtils`

`SystemConstants` is an interface exposing `PREFIX="system."`, `CGROUP_PREFIX="system.cgroup."`,
`NETWORK_PREFIX="system.network."` and the property names and defaults tabulated under [Configuration](#configuration).
`SystemUtils` (`.system.util`) and `MicrometerUtils` (`...micrometer.util`, `extends BaseUtils`) are abstract classes with
private constructors:

- `getCGroupDirectoryPath()`, `getNetworkStatsFilePath()` and `getMetricsCollectionInterval()` read the three `system.*`
  keys through `System.getProperty` (`Long.parseLong` for the interval) with the documented defaults.
- `readLines(Path)` wraps `IOUtils.readLines(Files.newInputStream(path))`; on `IOException` it logs
  `The file[path : '{}'] can't be read` and returns `StringUtils.EMPTY_STRING_ARRAY`.
- `async(Runnable)` submits to the shared scheduled pool (`null` ignored) and `getScheduledExecutor()` returns it —
  threads named `Micrometer-Async-*`, closed via `shutdownOnExit`, sized at class load by
  `Integer.getInteger("microsphere.metrics.micrometer.scheduled-executor.core-size", 1)` (the constant name itself is
  misspelled `MICROETER_...` in source).

### P6Spy: `MicrometerJdbcEventListener` (`.binder.jdbc.p6spy`)

```java
public MicrometerJdbcEventListener(MeterRegistry registry)
public MicrometerJdbcEventListener()   // registry stays null until setRegistry(registry)
public static void setRegistry(MeterRegistry registry)   // static volatile MeterRegistry field
public long getSlowSQLThresholdNanos() / public void setSlowSQLThresholdNanos(long nanos)
String resolveStatementType(String sql)   // package-private, exercised by tests
void addMetrics(String sql, long timeElapsedNanos, int batchCounts, @Nullable SQLException e)
SQL_SUCCESS_COUNT_METRIC_NAME = "microsphere.jdbc.sql.success.count";   SQL_FAILURE_COUNT_METRIC_NAME = "microsphere.jdbc.sql.failure.count"
SLOW_SQL_TIME_METRIC_NAME = "microsphere.jdbc.slow-sql.time";   SLOW_SQL_TIME_THRESHOLD_PROPERTY_NAME = "microsphere.jdbc.slow-sql.time.threshold"
DEFAULT_SLOW_SQL_TIME_THRESHOLD = TimeUnit.SECONDS.toNanos(1)   // 1_000_000_000 ns
```

It `extends com.p6spy.engine.logging.LoggingEventListener` and hooks `onAfterAnyExecute(StatementInformation, long,
SQLException)` (single statement, `batches` recorded as `0`), `onAfterExecuteBatch(StatementInformation, long, int[],
SQLException)` (`batches` = `updateCounts.length`) and the inherited `logElapsed(Loggable, long, Category, SQLException)`,
which is redirected to `P6LogQuery.logElapsed(connectionId, timeElapsedNanos, category, loggable)` instead of logging. All
three go through `execute(Runnable)`: it returns immediately when the static registry is `null`, otherwise the work is
handed to `MicrometerUtils.async(...)`, so recording never blocks the JDBC thread.

| Metric | Type | Description | Base unit | Tags |
|---|---|---|---|---|
| `microsphere.jdbc.sql.success.count` | Counter | executions whose SQL type resolved | — | `type`, `batches` |
| `microsphere.jdbc.sql.failure.count` | Counter | executions that also reported a `SQLException` | — | `type`, `batches` |
| `microsphere.jdbc.slow-sql.time` | Gauge, supplier `() -> timeElapsedNanos` | elapsed nanos of one slow statement | — | `sql`, `type`, `batches` |

`resolveStatementType` upper-cases the text before the first whitespace (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) and
returns `null` when the trimmed SQL has no whitespace after position 0 — then nothing is recorded, which is why the
integration test's bare `HELP` produces no meters. The threshold is read per instance with `Long.getLong` from
`microsphere.jdbc.slow-sql.time.threshold` (seconds, default `"1"`) and converted with `TimeUnit.SECONDS.toNanos`.

**Enabling it.** `src/main/resources/META-INF/services/com.p6spy.engine.event.JdbcEventListener` holds the single line
`io.microsphere.metrics.micrometer.instrument.binder.jdbc.p6spy.MicrometerJdbcEventListener`, so P6Spy's
`DefaultJdbcEventListenerFactory` instantiates it through the **no-arg** constructor — you must pass the registry with
`MicrometerJdbcEventListener.setRegistry(registry)`. The other half is a proxied datasource; the module's test datasource
`src/test/resources/META-INF/druid/h2.properties` shows it:

```properties
druid.url=jdbc:p6spy:h2:mem:test_db;DB_CLOSE_DELAY=-1
druid.driverClassName=com.p6spy.engine.spy.P6SpyDriver
druid.name=h2
druid.username=sa
```

No `spy.properties` is shipped anywhere in this repository, so P6Spy's defaults apply, and because the registry field is
`static volatile` and never cleared, the last `setRegistry` call wins for every listener in that class loader.

## Configuration

| Key | Default | Read by | Effect |
|---|---|---|---|
| `system.cgroup.dir` | `/sys/fs/cgroup/` | `SystemUtils.getCGroupDirectoryPath()` | base directory of the no-arg `CGroupMemoryMetrics`; `memory/` is appended |
| `system.network.stats.file` | `/proc/net/dev` | `SystemUtils.getNetworkStatsFilePath()` | file used by the no-arg `NetworkStatisticsMetrics` |
| `system.metrics.collection.interval` | `60000` ms | `SystemUtils.getMetricsCollectionInterval()` | refresh period of the network stats parser |
| `microsphere.jdbc.slow-sql.time.threshold` | `1` s | `Long.getLong(...)` per listener instance | above it `microsphere.jdbc.slow-sql.time` is registered |
| `microsphere.metrics.micrometer.scheduled-executor.core-size` | `1` | `Integer.getInteger(...)` at class load | size of the shared `Micrometer-Async-` pool |

All five are JVM system properties (`-Dkey=value`). `SystemConstants` annotates the `system.*` keys with
`@ConfigurationProperty(source = APPLICATION_SOURCE)` even though this module reads them with `System.getProperty`;
`microsphere-observability-metrics-spring-boot` resolves the same names from the Spring environment and gates its binders
with `microsphere.metrics.micrometer.cgroup.enabled` and `microsphere.metrics.micrometer.system.enabled`.

## Code examples

Binder contract (`AbstractMeterBinderTest`) and the fixture every binder test extends (`AbstractMetricsTest`):

```java
class AbstractMeterBinderTest extends AbstractMeterBinder {
    @Override protected boolean supports(MeterRegistry registry) { return true; }
    @Override protected void doBindTo(MeterRegistry registry) throws Throwable { throw new Throwable("For testing"); }
    @Test void testBindTo() { super.bindTo(new SimpleMeterRegistry()); }   // the Throwable must not propagate
}
public abstract class AbstractMetricsTest<T extends MeterBinder> {
    protected final MeterRegistry registry = new SimpleMeterRegistry();
    protected final T metrics = createMetrics();   // no-arg ctor of the test class' generic type argument
    @BeforeEach protected void bindTo() { metrics.bindTo(registry); }
}
```

cGroup memory (`CGroupMemoryMetricsTest`): `system.cgroup.dir` is repointed at classpath test data whose
`memory.limit_in_bytes` holds `6442450944` and whose `memory.stat` deliberately contains the non-numeric line
`unevictable for_test`, which is why two assertions are commented out upstream.

```java
@BeforeAll static void prepare() throws Throwable {
    setProperty(CGROUP_DIRECTORY_PATH_PROPERTY_NAME,
            get(getDefaultClassLoader().getResource("test-data/").toURI()).toAbsolutePath().toString());
}
@Test void test() {
    assertMeterPresent("system.cgroup.memory.usage_in_bytes");
    assertMeterPresent("system.cgroup.memory.limit_in_bytes");
    assertMeterPresent("system.cgroup.memory.stat.cache");
    assertMeterPresent("system.cgroup.memory.stat.hierarchical_memory_limit");
}
@Test void testOnNotFound() { assertFalse(new CGroupMemoryMetrics(get("not-found")).supports(this.registry)); }
```

Network stats (`NetworkStatisticsMetricsTest`) points `system.network.stats.file` at `test-data/memory/network.stats`
(rows `eth0`, `lo`, `docker0`, three `veth*`), binds through the fixture, then touches the file while a
`StandardFileWatchService` waits for `MODIFIED` and asserts the registry is not empty. Production usage is the same call
against the real path, yielding e.g. `system.network.receive.bytes{origin="NetworkStatisticsMetrics", interface="eth0"}`.

JMX (`MBeanMetricsTest`) shows the naming rule is yours alone; Sentinel (`SentinelMetricsTest`) shows both the gate and
the meter arithmetic — 100 resources produce 700 meters, and `sentinelMetrics.registry` stays `null` until
`addEntryCallback(new SentinelNodeEventPublisher())` has run:

```java
ObjectName objectNameToQuery = new ObjectName("java.util.logging:type=Logging");
new MBeanMetrics(objectNameToQuery, (mBeanName, mBeanInfo, mBeanAttributeInfo, attributeValue, registry) -> {
    if ("LoggerNames".equals(mBeanAttributeInfo.getName())) {
        registry.counter("logger.names.count").increment(((String[]) attributeValue).length);
    }
}).bindTo(registry);   // assertEquals(1, count of meters named logger.names.count)

addEntryCallback(new SentinelNodeEventPublisher());
sentinelMetrics.bindTo(registry);
for (int i = 0; i < 100; i++) { new SentinelTemplate().execute("test-resource-" + (i + 1), () -> {}); }
assertEquals(700, registry.getMeters().size());
```

P6Spy: the unit test drives the listener, the integration test proves the service file wired it into P6Spy.

```java
listener = new MicrometerJdbcEventListener(new SimpleMeterRegistry());
assertEquals("SELECT", listener.resolveStatementType("SELECT * FROM users;"));
assertNull(listener.resolveStatementType("HELP"));
listener.addMetrics("SELECT * FROM users;", 1000000000, 0, null);

CompoundJdbcEventListener compound = getStaticFieldValue(true, DefaultJdbcEventListenerFactory.class, "jdbcEventListener");
MicrometerJdbcEventListener listener = compound.getEventListeners()
        .get(compound.getEventListeners().size() - 1);      // discovered from META-INF/services
listener.setRegistry(new SimpleMeterRegistry());
listener.setSlowSQLThresholdNanos(1);
super.test();                                              // statements over jdbc:p6spy:h2:mem:test_db
listener.setSlowSQLThresholdNanos(DEFAULT_SLOW_SQL_TIME_THRESHOLD);
assertThrows(SQLException.class, () -> executeStatement(s -> s.executeUpdate("INSERT INTO users")));
assertDoesNotThrow(() -> executeStatement(s -> s.execute("HELP")));   // type unresolved -> no metrics
```

Utilities (`SystemUtilsTest`, `SystemConstantsTest`, `MicrometerUtilsTest`) pin the defaults and the reader behaviour:

```java
assertSame(DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE, getCGroupDirectoryPath());
assertSame(DEFAULT_NETWORK_STATS_FILE_PATH, getNetworkStatsFilePath());   // "/proc/net/dev"
assertEquals(parseLong(DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE), getMetricsCollectionInterval());
assertEquals(11, readLines(get(getResource("test-data/memory/network.stats").toURI())).length);
assertSame(EMPTY_STRING_ARRAY, readLines(get(getResource("test-data/memory").toURI())));  // directory -> IOException
async(() -> {}); async(null); assertNotNull(getScheduledExecutor());
```

## Metrics / log output reference

Every meter name and tag is tabulated in the [API reference](#api-reference); `system.` covers
`SystemMemoryMetrics` (5 gauges), `CGroupMemoryMetrics` (5 file gauges + up to 11 `memory.stat.` gauges) and
`NetworkStatisticsMetrics` (8 gauges per interface), `sentinel_` covers 7 gauges per resource and context,
`microsphere.jdbc.` covers the two counters and the conditional slow-SQL gauge, and `MBeanMetrics` emits whatever your
`MBeanAttributeMeterBinder` registers.

All log output goes through `io.microsphere.logging` and therefore lands in the host application's Logback/Log4j2:
`Current Metrics is not supported` (info), `Bind To MeterRegistry[{}] failed` (error),
`The CGroup memory directory[path: '{}'] does not exist!` (warn), `memory.stat Statistics : {} is not numeric` (warn),
`The Stats can't be parsed by the line {} : {}` (warn), `The Stats has been updated : {}` (trace),
`The file[path : '{}'] can't be read` (error).

## Notes and limitations

| Optional dependency | Used by | What degrades when absent |
|---|---|---|
| `io.micrometer:micrometer-core` | every class here | jar unusable — `MeterBinder`, `MeterRegistry`, `Gauge`, `Counter`, `TimeGauge`, `Tags`, `BaseUnits`, `NamedThreadFactory` are referenced directly, so class loading throws `NoClassDefFoundError` |
| `io.micrometer:micrometer-registry-prometheus` | no source file of this module | nothing here; optional so downstream Prometheus exporters resolve one version |
| `io.github.microsphere-projects:microsphere-java-core` | `Logger`, `ClassUtils`, `StringUtils`, `ObjectUtils`, `MapUtils`, `ListUtils`, `IOUtils`, `JmxUtils`, `BaseUtils`, `ThrowableSupplier`, `ExecutorUtils`, `@Nonnull`/`@Nullable`/`@ConfigurationProperty` | every binder fails to initialise; `AbstractMeterBinder` cannot build `commonTags` |
| `microsphere-observability-metrics-alibaba-sentinel` | `SentinelMetricsConstants` | `SentinelMetrics` cannot load; JMX, system and JDBC binders keep working |
| `io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` | `SentinelNodeEventPublisher`, `ClusterNodeAddedEvent`, `ClusterNodeAddedEventListener`, `SentinelUtils` | `SentinelMetrics` cannot load |
| `com.alibaba.csp:sentinel-core` | `ClusterNode`, `Constants.SENTINEL_VERSION`, `ProcessorSlotEntryCallback` | `SentinelMetrics` cannot load; no `sentinel_*` metrics |
| `com.alibaba.csp:sentinel-transport-simple-http` | not referenced by any source file | no direct effect; declared so Sentinel dashboard heartbeats exist for `SentinelMetrics` users |
| `p6spy:p6spy` | `MicrometerJdbcEventListener`, the `META-INF/services` file | listener and service entry become inert; no `microsphere.jdbc.*` metrics |
| `io.github.microsphere-projects:microsphere-annotation-processor` | build time only | no runtime impact; `@ConfigurationProperty` is not turned into metadata |

Compile-scope `microsphere-observability-metrics-commons` is the only non-optional project dependency, and no source file
in this module imports `io.microsphere.metrics.commons.*`; it is there for reactor ordering. Other source-verified quirks:

- `bindTo` swallows `Throwable`, so binder failures surface as one log line plus missing metrics.
- The five cGroup file gauges re-read at each collection while `memory.stat.*` gauges freeze their bind-time value; if a
  file disappears later, `readLines` returns an empty array and the supplier's `lines[0]` access throws during
  collection, not at bind time.
- `NetworkStatisticsMetrics` never unregisters vanished interfaces and holds strong references
  (`strongReference(true)` on six of eight gauges) to every `Stats` seen, so short-lived `veth*` names accumulate.
- `SentinelMetrics` accumulates one meter set per resource **and** per context, and meters are never removed.
- `microsphere.jdbc.slow-sql.time` is tagged with the full SQL text (cardinality hazard for non-parameterised SQL), and a
  failing statement increments **both** counters because the success counter is registered before the `SQLException` check.
- `MBeanMetrics` is a bind-time snapshot: no MBean rescan, no attribute refresh, no `origin` tag.
- The Javadoc of these classes carries `@since 1.0.0`, which matches no published artifact of this repository; the two
  released version lines are `0.1.x` and `0.2.x`.

## Related documents

- [Metrics Micrometer Binders — Developer Guide](../../developer-guide/current/metrics-micrometer.md)
- [Metrics Micrometer Binders — User Guide (0.1.x line)](../1.x/metrics-micrometer.md)
- [User Guide index](../README.md)
