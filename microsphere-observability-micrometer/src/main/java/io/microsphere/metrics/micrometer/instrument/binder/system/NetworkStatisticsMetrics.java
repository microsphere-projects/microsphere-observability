package io.microsphere.metrics.micrometer.instrument.binder.system;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.microsphere.annotation.Nullable;
import io.microsphere.metrics.micrometer.instrument.binder.AbstractMeterBinder;
import io.microsphere.metrics.micrometer.util.MicrometerUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

import static io.micrometer.core.instrument.Gauge.builder;
import static io.micrometer.core.instrument.Tags.concat;
import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.collection.MapUtils.newConcurrentHashMap;
import static io.microsphere.metrics.micrometer.instrument.binder.system.util.SystemUtils.getMetricsCollectionInterval;
import static io.microsphere.metrics.micrometer.instrument.binder.system.util.SystemUtils.getNetworkStatsFilePath;
import static io.microsphere.util.ObjectUtils.defaultIfNull;
import static io.microsphere.util.StringUtils.split;
import static java.lang.Long.parseLong;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllLines;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Network Statistics Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 */
public class NetworkStatisticsMetrics extends AbstractMeterBinder {

    private final ConcurrentMap<String, Stats> statsMap = newConcurrentHashMap(8);

    private final ScheduledExecutorService scheduledExecutorService;

    /**
     * The stats file path
     */
    private final Path statsFilePath;

    /**
     * The interval time of metrics collection in milliseconds.
     */
    private final long interval;

    private MeterRegistry registry;

    public NetworkStatisticsMetrics() {
        this(Paths.get(getNetworkStatsFilePath()), getMetricsCollectionInterval());
    }

    public NetworkStatisticsMetrics(Path statsFilePath, long interval) {
        this(statsFilePath, interval, null);
    }

    public NetworkStatisticsMetrics(Path statsFilePath, long interval, @Nullable ScheduledExecutorService scheduledExecutorService) {
        this(statsFilePath, interval, scheduledExecutorService, null);
    }

    public NetworkStatisticsMetrics(Path statsFilePath, long interval, @Nullable ScheduledExecutorService scheduledExecutorService, @Nullable Iterable<Tag> tags) {
        super(tags);
        this.statsFilePath = statsFilePath;
        this.interval = interval;
        this.scheduledExecutorService = defaultIfNull(scheduledExecutorService, MicrometerUtils::getScheduledExecutor);
    }

    private void bindStats() {
        List<String> lines = null;
        try {
            lines = readAllLines(this.statsFilePath, US_ASCII);
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Network stats file[path : '{}'] can't be read", this.statsFilePath);
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
                if (logger.isTraceEnabled()) {
                    logger.trace("The Stats has been updated : {}", stats);
                }
            }
        }
    }

    private void bindStats(Stats stats) {
        Iterable<Tag> newTags = concat(tags, "interface", stats.name);

        builder("network.receive.bytes", stats, Stats::getReceiveBytes)
                .tags(newTags)
                .description("Number of good received bytes")
                .strongReference(true)
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

        builder("network.receive.packets", stats, Stats::getReceivePackets)
                .tags(newTags)
                .description("Number of good packets received by the interface")
                .strongReference(true)
                .register(registry);

        builder("network.receive.errors", stats, Stats::getReceiveErrors)
                .tags(newTags)
                .description("Total number of bad packets received on this network device")
                .strongReference(true)
                .register(registry);

        builder("network.receive.drop", stats, Stats::getReceiveDrop)
                .tags(newTags)
                .description("Number of packets received but not processed")
                .strongReference(true)
                .register(registry);

        builder("network.transmit.bytes", stats, Stats::getTransmitBytes)
                .tags(newTags)
                .description("Number of good transmitted bytes")
                .strongReference(true)
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

        builder("network.transmit.packets", stats, Stats::getTransmitPackets)
                .tags(newTags)
                .description("Number of packets successfully transmitted")
                .strongReference(true)
                .register(registry);

        builder("network.transmit.errors", stats, Stats::getTransmitErrors)
                .tags(newTags).description("Total number of transmit problems")
                .strongReference(true)
                .register(registry);

        builder("network.transmit.drop", stats, Stats::getTransmitDrop)
                .tags(newTags).description("Number of packets dropped on their way to transmission")
                .strongReference(true)
                .register(registry);
    }

    @Override
    protected boolean supports(MeterRegistry registry) {
        return exists(this.statsFilePath);
    }

    @Override
    protected void doBindTo(MeterRegistry registry) throws Throwable {
        this.registry = registry;
        bindStats();
        bindStatsOnSchedule();
    }

    private void bindStatsOnSchedule() {
        ScheduledExecutorService scheduledExecutorService = this.scheduledExecutorService;
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.scheduleAtFixedRate(this::bindStats, 0, this.interval, MILLISECONDS);
        }
    }

    private Stats parseStats(String statsLine) {
        String[] nameAndData = split(statsLine, ':');
        Stats stats = null;
        if (nameAndData.length == 2) {

            String name = nameAndData[0].trim();
            String data = nameAndData[1].trim();
            int startIndex = -1;
            int endIndex = -1;
            char[] chars = data.toCharArray();
            int length = chars.length;

            List<String> values = newArrayList(16);

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

            stats.receiveBytes = parseLong(values.get(index++));
            stats.receivePackets = parseLong(values.get(index++));
            stats.receiveErrors = parseLong(values.get(index++));
            stats.receiveDrop = parseLong(values.get(index++));
            index += 4;
            stats.transmitBytes = parseLong(values.get(index++));
            stats.transmitPackets = parseLong(values.get(index++));
            stats.transmitErrors = parseLong(values.get(index++));
            stats.transmitDrop = parseLong(values.get(index++));
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
                this.receiveErrors = stats.receiveErrors;
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