package io.microsphere.metrics.micrometer.instrument.binder.system;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import io.microsphere.metrics.micrometer.instrument.binder.AbstractMeterBinder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.getProperty;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.Files.readAllLines;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * Network Statistics Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 */
@NonNullApi
@NonNullFields
public class NetworkStatisticsMetrics extends AbstractMeterBinder {

    public static final String STATS_FILE_PATH_PROPERTY_NAME = "microsphere.metrics.network.stats.file";

    public static final String DEFAULT_STATS_FILE_PATH = "/proc/net/dev";

    public static final Path STATS_FILE_PATH = get(getProperty(STATS_FILE_PATH_PROPERTY_NAME, DEFAULT_STATS_FILE_PATH));

    private static final Logger logger = LoggerFactory.getLogger(NetworkStatisticsMetrics.class);

    private final ConcurrentMap<String, Stats> statsMap = new ConcurrentHashMap<>(8);

    private final ScheduledExecutorService scheduledExecutorService;

    private MeterRegistry registry;

    public NetworkStatisticsMetrics() {
        this(null);
    }

    public NetworkStatisticsMetrics(ScheduledExecutorService scheduledExecutorService) {
        this(emptyList(), scheduledExecutorService);
    }

    public NetworkStatisticsMetrics(Iterable<Tag> tags, ScheduledExecutorService scheduledExecutorService) {
        super(tags);
        this.scheduledExecutorService = scheduledExecutorService == null ?
                newSingleThreadScheduledExecutor(new NamedThreadFactory("Network-Statistics-Metrics-Task-"))
                : scheduledExecutorService;
    }

    private void bindStats() {
        List<String> lines = null;
        try {
            lines = readAllLines(STATS_FILE_PATH, US_ASCII);
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Network stats file[path : '{}'] can't be read", STATS_FILE_PATH);
            }
            return;
        }

        int linesCount = lines.size();

        for (int i = 2; i < linesCount; i++) {
            String statsLine = lines.get(i);
            Stats stats = parseStats(statsLine);
            if (stats == null) {
                if (logger.isWarnEnabled()) {
                    logger.warn("The Stats can't be parsed by the line {} : {}", i + 1, statsLine);
                }
                continue;
            }
            Stats boundStats = statsMap.computeIfAbsent(stats.name, name -> {
                bindStats(stats);
                return stats;
            });
            if (boundStats.update(stats)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("The Stats has been updated : {}", stats);
                }
            }
        }
    }

    private void bindStats(Stats stats) {
        Iterable<Tag> newTags = Tags.concat(tags, "interface", stats.name);

        Gauge.builder("network.receive.bytes", stats, Stats::getReceiveBytes)
                .tags(newTags)
                .description("Number of good received bytes")
                .strongReference(true)
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

        Gauge.builder("network.receive.packets", stats, Stats::getReceivePackets)
                .tags(newTags)
                .description("Number of good packets received by the interface")
                .strongReference(true)
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

        Gauge.builder("network.receive.errors", stats, Stats::getReceiveErrors)
                .tags(newTags)
                .description("Total number of bad packets received on this network device")
                .strongReference(true)
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

        Gauge.builder("network.receive.drop", stats, Stats::getReceiveDrop)
                .tags(newTags)
                .description("Number of packets received but not processed")
                .strongReference(true)
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

        Gauge.builder("network.transmit.bytes", stats, Stats::getTransmitBytes)
                .tags(newTags)
                .description("Number of good transmitted bytes")
                .strongReference(true)
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

        Gauge.builder("network.transmit.packets", stats, Stats::getTransmitPackets)
                .tags(newTags)
                .description("Number of packets successfully transmitted")
                .strongReference(true)
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

        Gauge.builder("network.transmit.errors", stats, Stats::getTransmitErrors)
                .tags(newTags).description("Total number of transmit problems")
                .strongReference(true)
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

        Gauge.builder("network.transmit.drop", stats, Stats::getTransmitDrop)
                .tags(newTags).description("Number of packets dropped on their way to transmission")
                .strongReference(true)
                .baseUnit(BaseUnits.BYTES)
                .register(registry);
    }

    private void asyncBindStatsList() {
        Thread thread = new Thread(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                Path dir = STATS_FILE_PATH.getParent();
                dir.register(watchService, ENTRY_MODIFY);
                WatchKey key;
                while (true) {
                    key = watchService.poll(1, TimeUnit.SECONDS);
                    if (key == null) {
                        continue;
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path path = (Path) event.context();
                        if (STATS_FILE_PATH.equals(dir.resolve(path))) {
                            bindStats();
                        }
                    }
                    key.reset();
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });

        thread.setName("Network Statistics Thread");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    protected boolean supports(MeterRegistry registry) {
        return Files.exists(STATS_FILE_PATH);
    }

    @Override
    protected void doBindTo(MeterRegistry registry) throws Throwable {
        this.registry = registry;
        bindStats();
        bindStatsOnSchedule();
        // asyncBindStatsList();
    }

    private void bindStatsOnSchedule() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.scheduleAtFixedRate(this::bindStats, 0, 30, TimeUnit.SECONDS);
        }
    }

    private Stats parseStats(String statsLine) {
        String[] nameAndData = StringUtils.split(statsLine, ':');
        Stats stats = null;
        if (nameAndData.length == 2) {

            String name = nameAndData[0].trim();
            String data = nameAndData[1].trim();
            int startIndex = -1;
            int endIndex = -1;
            char[] chars = data.toCharArray();
            int length = chars.length;

            List<String> values = new ArrayList<>(16);

            for (int i = 0; i < length; i++) {
                char c = chars[i];
                boolean isDigit = Character.isDigit(c);
                if (isDigit && startIndex == -1) {
                    startIndex = i;
                }
                if (Character.isWhitespace(c)) {
                    endIndex = i;
                }
                if (isDigit && i == length - 1) {
                    endIndex = i + 1;
                }
                if (endIndex > startIndex && startIndex > -1) {
                    String value = new String(chars, startIndex, endIndex - startIndex);
                    values.add(value);
                    startIndex = -1;
                }
            }

            int index = 0;

            stats = new Stats(name);

            stats.receiveBytes = Long.parseLong(values.get(index++));
            stats.receivePackets = Long.parseLong(values.get(index++));
            stats.receiveErrors = Long.parseLong(values.get(index++));
            stats.receiveDrop = Long.parseLong(values.get(index++));
            index += 4;
            stats.transmitBytes = Long.parseLong(values.get(index++));
            stats.transmitPackets = Long.parseLong(values.get(index++));
            stats.transmitErrors = Long.parseLong(values.get(index++));
            stats.transmitDrop = Long.parseLong(values.get(index++));
        }
        return stats;
    }

    /**
     * Inter-|   Receive                                                |  Transmit
     * face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
     * lo:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
     * tunl0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
     * ip6tnl0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
     * eth0:    1392      16    0    0    0     0          0         0        0       0    0    0    0     0       0          0
     */
    private class Stats {

        private final String name;

        private long receiveBytes;

        private long receivePackets;

        private long receiveErrors;

        private long receiveDrop;

        private long transmitBytes;

        private long transmitPackets;

        private long transmitErrors;

        private long transmitDrop;

        private Stats(String name) {
            this.name = name;
        }

        public long getReceiveBytes() {
            return receiveBytes;
        }

        public long getReceivePackets() {
            return receivePackets;
        }

        public long getReceiveErrors() {
            return receiveErrors;
        }

        public long getReceiveDrop() {
            return receiveDrop;
        }

        public long getTransmitBytes() {
            return transmitBytes;
        }

        public long getTransmitPackets() {
            return transmitPackets;
        }

        public long getTransmitErrors() {
            return transmitErrors;
        }

        public long getTransmitDrop() {
            return transmitDrop;
        }

        private boolean update(Stats stats) {
            if (this == stats) {
                return false;
            }
            synchronized (this) {
                this.receiveBytes = stats.receiveBytes;
                this.receivePackets = stats.receivePackets;
                this.receiveErrors = stats.receivePackets;
                this.receiveDrop = stats.receiveDrop;

                this.transmitBytes = stats.transmitBytes;
                this.transmitPackets = stats.transmitPackets;
                this.transmitErrors = stats.transmitErrors;
                this.transmitDrop = stats.transmitDrop;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Stats{" +
                    "name='" + name + '\'' +
                    ", receiveBytes=" + receiveBytes +
                    ", receivePackets=" + receivePackets +
                    ", receiveErrors=" + receiveErrors +
                    ", receiveDrop=" + receiveDrop +
                    ", transmitBytes=" + transmitBytes +
                    ", transmitPackets=" + transmitPackets +
                    ", transmitErrors=" + transmitErrors +
                    ", transmitDrop=" + transmitDrop +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Stats stats = (Stats) o;
            return Objects.equals(name, stats.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}