package io.microsphere.metrics.micrometer.instrument.binder.system;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import io.microsphere.annotation.ConfigurationProperty;
import io.microsphere.metrics.micrometer.instrument.binder.AbstractMeterBinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.micrometer.core.instrument.Gauge.builder;
import static io.micrometer.core.instrument.binder.BaseUnits.BYTES;
import static io.microsphere.annotation.ConfigurationProperty.SYSTEM_PROPERTIES_SOURCE;
import static io.microsphere.collection.ListUtils.first;
import static io.microsphere.util.StringUtils.isNumeric;
import static java.lang.Long.parseLong;
import static java.lang.Long.valueOf;
import static java.lang.System.getProperty;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Paths.get;
import static java.util.Collections.emptyList;

/**
 * CGroup Memory Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see <a href="https://www.kernel.org/doc/Documentation/cgroup-v1/memory.txt">https://www.kernel.org/doc/Documentation/cgroup-v1/memory.txt</a>
 * @see JvmMemoryMetrics
 */
@NonNullApi
@NonNullFields
public class CGroupMemoryMetrics extends AbstractMeterBinder {

    private static final String METRIC_PREFIX = "cgroup.";

    /**
     * The Default Value of CGroup Memory Directory
     */
    public static final String DEFAULT_CGROUP_MEMORY_DIR_SYSTEM_PROPERTY_VALUE = "/sys/fs/cgroup/memory/";

    /**
     * The System Property Name of CGroup Memory Directory, default value : "/sys/fs/cgroup/memory/"
     */
    @ConfigurationProperty(
            defaultValue = DEFAULT_CGROUP_MEMORY_DIR_SYSTEM_PROPERTY_VALUE,
            source = SYSTEM_PROPERTIES_SOURCE
    )
    public static final String CGROUP_MEMORY_DIR_SYSTEM_PROPERTY_NAME = "cgroup.memory.dir";

    /**
     * The CGroup Memory Directory
     */
    public static final String CGROUP_MEMORY_DIR = getProperty(CGROUP_MEMORY_DIR_SYSTEM_PROPERTY_NAME, DEFAULT_CGROUP_MEMORY_DIR_SYSTEM_PROPERTY_VALUE);

    private static final Path ROOT_DIRECTORY_PATH = get(CGROUP_MEMORY_DIR);

    private static final Path MEMORY_STAT_FILE_PATH = ROOT_DIRECTORY_PATH.resolve("memory.stat");

    public CGroupMemoryMetrics() {
        super();
    }

    public CGroupMemoryMetrics(Iterable<Tag> tags) {
        super(tags);
    }

    @Override
    protected boolean supports(MeterRegistry registry) {
        if (!exists(ROOT_DIRECTORY_PATH)) {
            logger.info("The CGroup memory directory[path: '{}'] does not exist!", ROOT_DIRECTORY_PATH);
            return false;
        }
        return true;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        buildBytesGauge("memory.usage_in_bytes", registry);
        buildBytesGauge("memory.max_usage_in_bytes", registry);
        buildBytesGauge("memory.memsw.usage_in_bytes", registry);
        buildBytesGauge("memory.memsw.max_usage_in_bytes", registry);
        buildBytesGauge("memory.limit_in_bytes", registry);
        buildMemoryStatsGauge(registry);
    }

    private void buildBytesGauge(String fileName, MeterRegistry registry) {
        String metricName = METRIC_PREFIX + fileName;
        buildBytesGauge(metricName, () -> readFileAsLong(fileName), registry);
    }

    /**
     * cache 1462194176
     * rss 912412672
     * rss_huge 0
     * mapped_file 32768
     * swap 0
     *
     * @param registry
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

    private Map<String, String> loadMemoryStatistics() {
        List<String> lines = readAllLines(MEMORY_STAT_FILE_PATH);
        int length = lines.size();
        Map<String, String> memoryStatistics = new LinkedHashMap<>(length);
        for (int i = 0; i < length; i++) {
            String line = lines.get(i);
            String[] keyAndValue = line.split(" ");
            if (keyAndValue.length == 2) {
                memoryStatistics.put(keyAndValue[0], keyAndValue[1]);
            }
        }
        return memoryStatistics;
    }

    private void buildMemoryStatsGauge(String statistic, Map<String, String> memoryStatistics, MeterRegistry registry) {
        String value = memoryStatistics.get(statistic);
        if (value == null) {
            logger.warn("memory.stat Statistics : {} was not found", statistic);
            return;
        }
        if (!isNumeric(value)) {
            logger.warn("memory.stat Statistics : {} is not numeric", statistic);
            return;
        }
        String metricName = METRIC_PREFIX + "memory.stat." + statistic;
        buildBytesGauge(metricName, () -> parseLong(value), registry);
    }

    private void buildBytesGauge(String name, Supplier<Number> supplier, MeterRegistry registry) {
        builder(name, supplier)
                .tags(tags)
                .baseUnit(BYTES)
                .register(registry);
    }

    private Long readFileAsLong(String fileName) {
        return readFileAsLong(ROOT_DIRECTORY_PATH.resolve(fileName));
    }

    private Long readFileAsLong(Path file) {
        if (!exists(file) || !isReadable(file)) {
            logger.debug("File[path : {}] does not exist !", file);
            return valueOf(-1L);
        }
        return parseLong(readFileContent(file));
    }

    private String readFileContent(Path filePath) {
        List<String> lines = readAllLines(filePath);
        return first(lines);
    }

    private List<String> readAllLines(Path filePath) {
        List<String> lines;
        try {
            lines = Files.readAllLines(filePath);
        } catch (Throwable e) {
            logger.warn("File [path : {}] can't be read", filePath, e);
            lines = emptyList();
        }
        return lines;
    }
}
