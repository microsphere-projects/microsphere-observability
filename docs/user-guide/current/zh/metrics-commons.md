# metrics-commons 用户文档

[English version](../metrics-commons.md) · [用户文档索引](./README.md)

## 概述

`microsphere-observability-metrics-commons` 是 Microsphere Observability 指标族中的“词汇表”模块。它不含采集器、注册表
或 Spring 代码，只提供两个类型来描述*一个指标是什么*，从而让下游模块（`metrics-alibaba-sentinel`、
`metrics-micrometer`、`metrics-prometheus`）共享同一份指标定义，而不必为每个后端重复定义。

- `MetricFamily` —— 不可变、与后端无关的 Prometheus 指标族描述符：名称、单位、类型、help 文本和标签名。
- `MetricType` —— Prometheus 指标类型枚举。

两者都是纯数据载体。把它们转换成具体客户端模型（Prometheus 的 `MetricFamilyDescriptor`、Micrometer 的 `Meter`）
的逻辑位于依赖本模块的其它模块中。

## 版本与兼容性

| 属性 | 值 |
|:---|:---|
| 分析分支 | `release` |
| Maven 坐标 | `io.github.microsphere-projects:microsphere-observability-metrics-commons` |
| 版本线 | `0.2.x`（reactor `${revision}` = `0.2.4-SNAPSHOT`，最新已发布标签 `0.2.3`） |
| Java 版本 | `17`（由 reactor 根 pom 声明） |
| Javadoc | [io.github.microsphere-projects/microsphere-observability-metrics-commons](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-commons) |
| 两个类的 `@since` | `1.0.0` |

本模块不依赖 Spring，因此不受 Spring Boot / Spring Cloud profile 矩阵影响。以下版本继承自 reactor 父工程
`microsphere-spring-cloud-parent:0.2.26`，它通过构建 profile 选择 Spring Boot/Spring Cloud（默认项加粗）：

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|:---|:---|:---|:---|
| `spring-cloud-2022` | 3.0.13 | 2022.0.5 | 2022.0.0.2 |
| `spring-cloud-2023` | 3.3.13 | 2023.0.6 | 2023.0.3.4 |
| `spring-cloud-2024` | 3.4.13 | 2024.0.3 | 2023.0.3.4 |
| **`spring-cloud-2025`（默认）** | **3.5.15** | **2025.0.3** | **2025.0.0.0** |
| `spring-cloud-2025.1` | 4.0.8 | 2025.1.3 | 2025.1.0.0 |

Prometheus 客户端兼容性只影响*渲染*这些描述符的模块：该分支的 reactor 将 `io.prometheus:prometheus-metrics-bom`
管理为 `1.9.0`（见 [metrics-prometheus](./metrics-prometheus.md)）。

## 安装

`metrics-alibaba-sentinel`、`metrics-micrometer`、`metrics-prometheus` 都以 `compile` 作用域声明本模块，因此引入其中
任意一个即可传递获得。只有当你需要自建指标目录时才直接声明它：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-observability-dependencies</artifactId>
            <version>${microsphere-observability.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.microsphere-projects</groupId>
        <artifactId>microsphere-observability-metrics-commons</artifactId>
    </dependency>
</dependencies>
```

版本由 `microsphere-observability-dependencies` 统一锁定，因此模块依赖上无需写 `<version>`。

## 快速上手

```java
import io.microsphere.metrics.commons.MetricFamily;
import io.microsphere.metrics.commons.MetricType;

import static java.util.Set.of;

MetricFamily family = MetricFamily.builder()
        .name("sentinel_rt")
        .unit("millseconds")
        .type(MetricType.GAUGE)
        .help("The Response Time (RT) of the Sentinel Resource")
        .labelNames(of("sentinel_resource", "sentinel_context", "sentinel_resource_type", "sentinel_version"))
        .build();

String name = family.getName();   // "sentinel_rt"
String unit = family.getUnit();   // "millseconds"
MetricType type = family.getType(); // GAUGE
```

## API 说明

### `io.microsphere.metrics.commons.MetricType`

`public enum MetricType` —— Prometheus 指标类型，参照
[prometheus.io/docs/concepts/metric_types](https://prometheus.io/docs/concepts/metric_types/) 定义。

| 枚举常量 | 含义 |
|:---|:---|
| `COUNTER` | 单调递增的值 |
| `GAUGE` | 可增可减的值 |
| `HISTOGRAM` | 分桶的观测值 |
| `SUMMARY` | 观测值的分位数摘要 |
| `INFO` | 常量型信息指标 |
| `STATESET` | 一组互斥状态，同时只有一个生效 |
| `UNKNOWN` | 类型未知——为不提供类型的指标源准备的兜底值 |

该枚举刻意与 Prometheus 客户端模型保持名称一致：`metrics-prometheus` 通过
`io.prometheus.metrics.model.registry.MetricType.valueOf(metricType.name())` 做桥接，因此若在客户端枚举中找不到同名
常量，该指标族就无法被 Prometheus 模块渲染。本仓库提供的所有指标族都使用 `GAUGE`。

### `io.microsphere.metrics.commons.MetricFamily`

`public class MetricFamily` —— 指标族模型；类级 javadoc 将其定位在
`io.prometheus.metrics.model.snapshots.MetricFamilyDescriptor` 与旧版
`io.prometheus.client.Collector.MetricFamilySamples` 之间。

| 成员 | 签名 | 说明 |
|:---|:---|:---|
| `builder()` | `public static Builder builder()` | 创建空的构造器——获取实例的唯一公开方式 |
| `MetricFamily(...)` | `MetricFamily(String name, String unit, MetricType type, String help, Set<String> labelNames)` | 包级私有构造器，`io.microsphere.metrics.commons` 之外不可调用 |
| `getName()` | `public String getName()` | 原始书写的指标名，尚未做 Prometheus 规范化或追加单位后缀 |
| `getUnit()` | `public String getUnit()` | 基本单位，可能为 `null`；渲染模块会把它追加到名称后 |
| `getType()` | `public MetricType getType()` | 指标类型 |
| `getHelp()` | `public String getHelp()` | 供人阅读的 HELP 文本 |
| `getLabelNames()` | `public Set<String> getLabelNames()` | 返回构造时传入的标签名集合（不是标签值） |

`MetricFamily.Builder`（公共嵌套类）：

| 方法 | 返回 | 说明 |
|:---|:---|:---|
| `name(String name)` | `Builder` | 链式调用 |
| `unit(String unit)` | `Builder` | 可选；`null` 或空白表示“无单位” |
| `type(MetricType type)` | `Builder` | 类型系统上不强制——没有任何校验 |
| `help(String help)` | `Builder` | 可选 |
| `labelNames(Set<String> labelNames)` | `Builder` | 该 Set 按引用保存，不做拷贝 |
| `build()` | `MetricFamily` | 永不返回 `null`；未设置的字段保持 `null` |

可空性与校验：构造器不做任何检查，构造方法直接赋值。因此完全可以构造出一个 `name` 或 `type` 为 `null` 的指标族；
有效性是在描述符被消费的地方保证的，而不是在这一层。这正是现有代码依赖的契约——`SentinelMetricUtitls` 显式提供了
每一个字段。

## 代码示例

本模块自身没有单元测试；下面的参考用法来自 `metrics-alibaba-sentinel` 及其测试 `SentinelMetricUtitlsTest`
（见 [metrics-alibaba-sentinel](./metrics-alibaba-sentinel.md)）。

定义指标族目录（生产代码）：

```java
public static final Set<String> REQUIRED_LABEL_NAMES = Set.of(
        RESOURCE_LABEL_NAME, CONTEXT_LABEL_NAME, RESOURCE_TYPE_LABEL_NAME, VERSION_LABEL_NAME);

public static final List<MetricFamily> METRIC_FAMILIES = ofList(
        builder().name(RT_METRIC_NAME).unit("millseconds").type(GAUGE)
                .help("The Response Time (RT) of the Sentinel Resource")
                .labelNames(REQUIRED_LABEL_NAMES).build(),
        builder().name(CONCURRENCY_METRIC_NAME).type(GAUGE)
                .help("The Concurrency of the Sentinel Resource")
                .labelNames(REQUIRED_LABEL_NAMES).build()
        // …还有 5 个指标族
);
```

校验指标族目录（测试代码，出自 `SentinelMetricUtitlsTest#testConstants`）：

```java
assertEquals(4, REQUIRED_LABEL_NAMES.size());
assertEquals(7, METRIC_FAMILIES_SIZE);
assertEquals(RT_METRIC_NAME, METRIC_FAMILIES.get(0).getName());
assertEquals(EXCEPTION_QPS_METRIC_NAME, METRIC_FAMILIES.get(6).getName());
```

单位会影响最终导出的指标名，同一测试也锁定了这一行为：

```java
assertEquals(RT_METRIC_NAME + "_millseconds", buildMetricName(getMetricFamily(0)));
assertEquals(CONCURRENCY_METRIC_NAME, buildMetricName(getMetricFamily(1))); // 无单位 -> 名称不变
```

## 注意事项与限制

- 字段只读，但类未定义 `equals`、`hashCode`、`toString`，因此是标识语义而非值语义。若你需要按实例比较或缓存，请像
  `METRIC_FAMILIES` 那样为每个指标只保留一个实例。
- `labelNames(Set)` 保存的是传入引用的本身。`SentinelMetricUtitls` 交给它的是不可变的 `keySet()` 视图，这是推荐写法；
  若别处仍持有可变 Set，一个“已冻结”的描述符会被改动。
- 不做名称校验：Prometheus 规范化（`sanitizeMetricName`）发生在 `metrics-prometheus`。因此诸如 `sentinel.rt` 的名称
  会在本层原样保留，直到渲染时才被改写。
- 本模块的依赖 `microsphere-java-core` 与 `microsphere-annotation-processor` 都是 `optional`，所以引入它不会给运行时
  classpath 增加任何内容。

## 相关文档

- [English User Guide](../metrics-commons.md)
- [开发者文档 —— metrics-commons](../../developer-guide/current/zh/metrics-commons.md)
- [用户文档 —— 1.x / release-1.x 分支](../1.x/zh/metrics-commons.md)
- [本分支用户文档索引](./README.md)
