package io.microsphere.metrics.micrometer.instrument.binder.system;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.microsphere.metrics.micrometer.instrument.binder.AbstractMeterBinder;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

import static io.micrometer.core.instrument.Gauge.builder;
import static io.micrometer.core.instrument.binder.BaseUnits.BYTES;
import static io.microsphere.collection.MapUtils.newFixedLinkedHashMap;
import static io.microsphere.constants.SymbolConstants.SPACE_CHAR;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.CGROUP_PREFIX;
import static io.microsphere.metrics.micrometer.instrument.binder.system.util.SystemUtils.getCGroupDirectoryPath;
import static io.microsphere.metrics.micrometer.instrument.binder.system.util.SystemUtils.readLines;
import static io.microsphere.util.StringUtils.isNumeric;
import static java.lang.Long.parseLong;
import static java.nio.file.Files.exists;
import static java.nio.file.Paths.get;
import static java.util.Collections.emptyList;

/**
 * CGroup Memory Metrics binder for Micrometer.
 * <p>
 * This class collects and exposes memory metrics from Linux CGroup (Control Groups) v1 filesystem.
 * It reads memory usage statistics from {@code /sys/fs/cgroup/memory/} directory and exposes them
 * as Prometheus-compatible metrics.
 * </p>
 * <p>
 * Supported metrics include:
 * </p>
 * <ul>
 *   <li>{@code cgroup_memory_usage_in_bytes} - Current memory usage</li>
 *   <li>{@code cgroup_memory_max_usage_in_bytes} - Maximum memory usage observed</li>
 *   <li>{@code cgroup_memory_memsw_usage_in_bytes} - Memory + swap usage</li>
 *   <li>{@code cgroup_memory_limit_in_bytes} - Memory limit</li>
 *   <li>{@code cgroup_memory_stat_cache}, {@code cgroup_memory_stat_rss}, etc. - Detailed memory stats</li>
 * </ul>
 * <h3>Example Usage</h3>
 * <pre>{@code
 * // Create with default CGroup path
 * CGroupMemoryMetrics metrics = new CGroupMemoryMetrics();
 * metrics.bindTo(meterRegistry);
 *
 * // Or customize path
 * Path customPath = Paths.get("/custom/cgroup/path");
 * CGroupMemoryMetrics metrics2 = new CGroupMemoryMetrics(customPath);
 * }</pre>
 * <h3>Example Configuration</h3>
 * <pre>{@code
 * // application.properties
 * system.cgroup.dir=/sys/fs/cgroup
 * }</pre>
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see <a href="https://www.kernel.org/doc/Documentation/cgroup-v1/memory.txt">CGroup Memory Documentation</a>
 * @see JvmMemoryMetrics
 */
public class CGroupMemoryMetrics extends AbstractMeterBinder {

    /**
     * The prefix for all CGroup memory metrics: "system.cgroup."
     */
    public static final String METRIC_PREFIX = CGROUP_PREFIX;

    private final Path memoryDirectoryPath;

    private final Path memoryStaFilePath;

    /**
     * Constructs a new {@code CGroupMemoryMetrics} instance with default CGroup directory.
     * <p>
     * Uses the CGroup directory path configured via {@link SystemConstants#CGROUP_DIRECTORY_PATH_PROPERTY_NAME}.
     * </p>
     * <h3>Example Behavior</h3>
     * <pre>{@code
     * // By default:
     * memoryDirectoryPath = /sys/fs/cgroup/memory
     * memoryStaFilePath = /sys/fs/cgroup/memory/memory.stat
     *
     * // If customized in properties:
     * // system.cgroup.dir=/custom/cgroup
     * // memoryDirectoryPath = /custom/cgroup/memory
     * }</pre>
     */
    public CGroupMemoryMetrics() {
        this(get(getCGroupDirectoryPath()));
    }

    /**
     * Constructs a new {@code CGroupMemoryMetrics} instance with specified CGroup directory.
     * <p>
     * Uses the provided directory path as the base for memory statistics collection.
     * </p>
     * <h3>Example Usage</h3>
     * <pre>{@code
     * // Use custom CGroup path
     * Path cgroupPath = Paths.get("/custom/cgroup/path");
     * CGroupMemoryMetrics metrics = new CGroupMemoryMetrics(cgroupPath);
     * metrics.bindTo(registry);
     *
     * // Resulting metrics will have path-specific tags or defaults
     * }</pre>
     *
     * @param cgroupDirectoryPath the root CGroup directory path
     */
    public CGroupMemoryMetrics(Path cgroupDirectoryPath) {
        this(cgroupDirectoryPath, emptyList());
    }

    /**
     * Constructs a new {@code CGroupMemoryMetrics} instance with specified CGroup directory and tags.
     * <p>
     * Allows customization of common tags applied to all collected metrics.
     * </p>
     * <h3>Example Usage</h3>
     * <pre>{@code
     * // Create with custom tags
     * List<Tag> tags = Arrays.asList(
     *     Tag.of("container", "my-app"),
     *     Tag.of("environment", "production")
     * );
     * CGroupMemoryMetrics metrics = new CGroupMemoryMetrics(cgroupPath, tags);
     * metrics.bindTo(registry);
     *
     * // Resulting metrics will include tags:
     * // system_cgroup_memory_usage_in_bytes{container="my-app", environment="production"} 123456789
     * }</pre>
     *
     * @param cgroupDirectoryPath the root CGroup directory path
     * @param tags                list of common tags to apply to all metrics
     */
    public CGroupMemoryMetrics(Path cgroupDirectoryPath, Iterable<Tag> tags) {
        super(tags);
        this.memoryDirectoryPath = cgroupDirectoryPath.resolve("memory");
        this.memoryStaFilePath = memoryDirectoryPath.resolve("memory.stat");
    }

    /**
     * Checks if this metrics binder is supported on the current platform.
     * <p>
     * This method verifies that the CGroup memory directory exists at the configured path.
     * If the directory does not exist, a warning is logged and the binder returns false.
     * </p>
     * <h3>Example Behavior</h3>
     * <pre>{@code
     * // When CGroup is enabled:
     * // Returns true (memory directory exists at /sys/fs/cgroup/memory)
     *
     * // When CGroup is disabled or container not using CGroups:
     * // Logs warning and returns false
     * }</pre>
     *
     * @param registry the meter registry (unused but required by contract)
     * @return true if CGroup memory directory exists, false otherwise
     */
    @Override
    protected boolean supports(MeterRegistry registry) {
        if (!exists(memoryDirectoryPath)) {
            logger.warn("The CGroup memory directory[path: '{}'] does not exist!", memoryDirectoryPath);
            return false;
        }
        return true;
    }

    /**
     * Binds all CGroup memory metrics to the provided meter registry.
     * <p>
     * This method creates and registers various memory usage gauges including:
     * </p>
     * <ul>
     *   <li>Current memory usage</li>
     *   <li>Maximum memory usage observed</li>
     *   <li>Memory + swap usage</li>
     *   <li>Memory limit</li>
     *   <li>Detailed statistics from memory.stat (cache, RSS, mapped file, etc.)</li>
     * </ul>
     * <h3>Example Metrics Registered</h3>
     * <pre>{@code
     * system_cgroup_memory_usage_in_bytes{container="myapp"} 123456789
     * system_cgroup_memory_max_usage_in_bytes{container="myapp"} 987654321
     * system_cgroup_memory_stat_cache{container="myapp"} 45678901
     * system_cgroup_memory_stat_rss{container="myapp"} 34567890
     * }</pre>
     *
     * @param registry the Micrometer meter registry to bind metrics to
     */
    @Override
    protected void doBindTo(MeterRegistry registry) {
        buildBytesGauge("memory.usage_in_bytes", registry);
        buildBytesGauge("memory.max_usage_in_bytes", registry);
        buildBytesGauge("memory.memsw.usage_in_bytes", registry);
        buildBytesGauge("memory.memsw.max_usage_in_bytes", registry);
        buildBytesGauge("memory.limit_in_bytes", registry);
        buildMemoryStatsGauge(registry);
    }

    /**
     * Builds gauge metrics for individual memory usage files.
     * <p>
     * This method reads memory statistics from a file within the CGroup memory directory
     * and registers it as a Prometheus gauge metric with bytes unit.
     * </p>
     * <h3>Example Metrics Built</h3>
     * <pre>{@code
     * // For "memory.usage_in_bytes":
     * system_cgroup_memory_usage_in_bytes 123456789
     *
     * // For "memory.limit_in_bytes":
     * system_cgroup_memory_limit_in_bytes 5368709120
     * }</pre>
     *
     * @param fileName   the name of the memory stat file (e.g., "memory.usage_in_bytes")
     * @param registry   the Micrometer meter registry to bind metric to
     */
    private void buildBytesGauge(String fileName, MeterRegistry registry) {
        String metricName = METRIC_PREFIX + fileName;
        buildBytesGauge(metricName, () -> readFileAsLong(fileName), registry);
    }

    /**
     * Builds gauge metrics from detailed memory statistics file.
     * <p>
     * Parses {@code memory.stat} file and creates separate gauges for each statistic including:
     * cache, RSS (Resident Set Size), mapped file, swap, active/inactive pages, etc.
     * </p>
     * <h3>Example Statistics Created</h3>
     * <pre>{@code
     * system_cgroup_memory_stat_cache          : Pages underLRU list cached
     * system_cgroup_memory_stat_rss            : Resident Set Size
     * system_cgroup_memory_stat_mapped_file    : Mapped file pages
     * system_cgroup_memory_stat_swap           : Swap usage
     * }</pre>
     *
     * @param registry the Micrometer meter registry to bind metrics to
     */
    private void buildMemoryStatsGauge(MeterRegistry registry) {
        Map<String, String> memoryStatistics = loadMemoryStatistics();
        buildMemoryStatsGauge("cache", memoryStatistics, registry);
        buildMemoryStatsGauge("rss", memoryStatistics, registry);
        buildMemoryStatsGauge("mapped_file", memoryStatistics, registry);
        buildMemoryStatsGauge("swap", memoryStatistics, registry);
        buildMemoryStatsGauge("active_anon", memoryStatistics, registry);
        buildMemoryStatsGauge("inactive_anon", memoryStatistics, registry);
        buildMemoryStatsGauge("active_file", memoryStatistics, registry);
        buildMemoryStatsGauge("inactive_file", memoryStatistics, registry);
        buildMemoryStatsGauge("unevictable", memoryStatistics, registry);
        buildMemoryStatsGauge("hierarchical_memory_limit", memoryStatistics, registry);
        buildMemoryStatsGauge("hierarchical_memsw_limit", memoryStatistics, registry);
    }

    /**
     * Loads memory statistics from the memory.stat file.
     * <p>
     * Parses the CGroup memory.stat file which contains space-separated key-value pairs
     * representing various memory usage counters. Each line is in the format "key value".
     * </p>
     * <h3>Example File Content</h3>
     * <pre>{@code
     * nr_free_pages 1024
     * pgpgin 5678901
     * pgpgout 2345678
     * rss 123456789
     * mapped_file 45678901
     * }</pre>
     *
     * <h3>Returned Map Example</h3>
     * <pre>{@code
     * {
     *   "nr_free_pages" -> "1024",
     *   "rss" -> "123456789",
     *   "mapped_file" -> "45678901"
     * }
     * }</pre>
     *
     * @return immutable map of memory statistic names to their string values
     */
    private Map<String, String> loadMemoryStatistics() {
        String[] lines = readLines(memoryStaFilePath);
        int size = lines.length;
        Map<String, String> memoryStatistics = newFixedLinkedHashMap(size);
        for (int i = 0; i < size; i++) {
            String line = lines[i];
            int spaceIndex = line.indexOf(SPACE_CHAR);
            if (spaceIndex == -1) {
                continue;
            }
            String name = line.substring(0, spaceIndex);
            String value = line.substring(spaceIndex + 1);
            memoryStatistics.put(name, value);
        }
        return memoryStatistics;
    }

    /**
     * Builds a gauge metric for a specific memory statistic.
     * <p>
     * Retrieves the numeric value for the given statistic and registers it as a gauge metric.
     * Skips non-numeric values with a warning log.
     * </p>
     * <h3>Example Metric Created</h3>
     * <pre>{@code
     * // If statistic="cache" and value="45678901":
     * system_cgroup_memory_stat_cache{container="myapp"} 45678901
     * }</pre>
     *
     * @param statistic         the name of the statistic (e.g., "cache", "rss", "mapped_file")
     * @param memoryStatistics  the map of all memory statistics from memory.stat
     * @param registry          the Micrometer meter registry to bind metric to
     */
    private void buildMemoryStatsGauge(String statistic, Map<String, String> memoryStatistics, MeterRegistry registry) {
        String value = memoryStatistics.get(statistic);
        if (!isNumeric(value)) {
            logger.warn("memory.stat Statistics : {} is not numeric", statistic);
            return;
        }
        String metricName = METRIC_PREFIX + "memory.stat." + statistic;
        buildBytesGauge(metricName, () -> parseLong(value), registry);
    }

    /**
     * Builds a Prometheus gauge metric with bytes unit.
     * <p>
     * Creates a gauge that returns a numeric value from the provided supplier and registers
     * it with the common tags and bytes base unit.
     * </p>
     * <h3>Example Usage</h3>
     * <pre>{@code
     * buildBytesGauge("custom_metric", () -> 123456789L, registry);
     *
     * // Result: custom_metric_bytes{common_tag="value"} 123456789
     * }</pre>
     *
     * @param name      the metric name (without prefix)
     * @param supplier  function that provides the metric value
     * @param registry  the Micrometer meter registry to bind metric to
     */
    private void buildBytesGauge(String name, Supplier<Number> supplier, MeterRegistry registry) {
        builder(name, supplier)
                .tags(commonTags)
                .baseUnit(BYTES)
                .register(registry);
    }

    /**
     * Reads a long value from a file within the memory directory.
     * <p>
     * This method constructs the full path by resolving the relative fileName against
     * the memoryDirectoryPath, then delegates to the more specific {@link #readFileAsLong(Path)}.
     * </p>
     * <h3>Example Behavior</h3>
     * <pre>{@code
     * // fileName = "memory.usage_in_bytes"
     * // memoryDirectoryPath = /sys/fs/cgroup/memory
     * // Resolves to: /sys/fs/cgroup/memory/memory.usage_in_bytes
     * // Returns: 123456789
     * }</pre>
     *
     * @param fileName the relative filename within the memory directory
     * @return the long value read from the file
     */
    private Long readFileAsLong(String fileName) {
        return readFileAsLong(memoryDirectoryPath.resolve(fileName));
    }

    /**
     * Reads and parses a long value from the specified file path.
     * <p>
     * Reads the first line of the file, trims whitespace, and parses it as a long integer.
     * Throws exception if file cannot be read or content is not a valid number.
     * </p>
     * <h3>Example File Content</h3>
     * <pre>{@code
     * 123456789
     * }
     * </pre>
     *
     * <h3>Example Return Value</h3>
     * <pre>{@code
     * readFileAsLong(pathToUsageFile) -> 123456789L
     * }</pre>
     *
     * @param file the absolute path to the file containing the numeric value
     * @return the parsed long value from the file
     */
    private Long readFileAsLong(Path file) {
        return parseLong(readFileContent(file));
    }

    /**
     * Reads the content from a file and returns the first line.
     * <p>
     * Assumes single-line files where the first line contains the entire value.
     * Used for reading simple CGroup counter files.
     * </p>
     * <h3>Example Behavior</h3>
     * <pre>{@code
     * Path path = Paths.get("/sys/fs/cgroup/memory/memory.usage_in_bytes");
     * String content = readFileContent(path);
     * // content = "123456789"
     * }</pre>
     *
     * @param filePath the path to the file to read
     * @return the first line of the file content
     */
    private String readFileContent(Path filePath) {
        String[] lines = readLines(filePath);
        return lines[0];
    }
}