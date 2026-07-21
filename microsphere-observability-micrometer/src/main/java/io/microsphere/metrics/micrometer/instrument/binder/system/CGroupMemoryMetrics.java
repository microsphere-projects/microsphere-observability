package io.microsphere.metrics.micrometer.instrument.binder.system;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.microsphere.metrics.micrometer.instrument.binder.AbstractMeterBinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.micrometer.core.instrument.Gauge.builder;
import static io.micrometer.core.instrument.binder.BaseUnits.BYTES;
import static io.microsphere.collection.ListUtils.first;
import static io.microsphere.collection.MapUtils.newFixedLinkedHashMap;
import static io.microsphere.metrics.micrometer.instrument.binder.system.util.SystemUtils.getCGroupDirectoryPath;
import static io.microsphere.util.StringUtils.isNumeric;
import static io.microsphere.util.StringUtils.split;
import static java.lang.Long.parseLong;
import static java.lang.Long.valueOf;
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
public class CGroupMemoryMetrics extends AbstractMeterBinder {

    public static final String METRIC_PREFIX = "cgroup.";

    private final Path memoryDirectoryPath;

    private final Path memoryStaFilePath;

    public CGroupMemoryMetrics() {
        this(get(getCGroupDirectoryPath()));
    }

    public CGroupMemoryMetrics(Path cgroupDirectoryPath) {
        this(cgroupDirectoryPath, emptyList());
    }

    public CGroupMemoryMetrics(Path cgroupDirectoryPath, Iterable<Tag> tags) {
        super(tags);
        this.memoryDirectoryPath = cgroupDirectoryPath.resolve("memory");
        this.memoryStaFilePath = memoryDirectoryPath.resolve("memory.stat");
    }

    @Override
    protected boolean supports(MeterRegistry registry) {
        if (!exists(memoryDirectoryPath)) {
            logger.info("The CGroup memory directory[path: '{}'] does not exist!", memoryDirectoryPath);
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
        List<String> lines = readAllLines(memoryStaFilePath);
        int size = lines.size();
        Map<String, String> memoryStatistics = newFixedLinkedHashMap(size);
        for (int i = 0; i < size; i++) {
            String line = lines.get(i);
            String[] keyAndValue = split(line, " ");
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
        return readFileAsLong(memoryDirectoryPath.resolve(fileName));
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
