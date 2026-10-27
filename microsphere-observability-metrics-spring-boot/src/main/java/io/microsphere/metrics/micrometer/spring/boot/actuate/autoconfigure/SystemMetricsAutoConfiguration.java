/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure;

import io.microsphere.annotation.ConfigurationProperty;
import io.microsphere.metrics.micrometer.instrument.binder.system.NetworkStatisticsMetrics;
import io.microsphere.metrics.micrometer.instrument.binder.system.SystemMemoryMetrics;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerAvailable;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnPlatformMXBeanAvailable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;
import static io.microsphere.constants.PropertyConstants.ENABLED_PROPERTY_NAME;
import static io.microsphere.constants.SymbolConstants.DOT;
import static io.microsphere.metrics.micrometer.instrument.binder.system.SystemMemoryMetrics.OPERATING_SYSTEM_MXBEAN_CLASS_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.DEFAULT_NETWORK_STATS_FILE_PATH;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.METRICS_COLLECTION_INTERVAL_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.NETWORK_STATS_FILE_PATH_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SystemMetricsAutoConfiguration.SYSTEM_METRICS_ENABLED_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled.PREFIX;

/**
 * The Auto-Configuration class for System Metrics (Memory, Network).
 * <p>
 * This configuration provides automatic bean creation for system-level metrics including:
 * </p>
 * <ul>
 *   <li>{@link NetworkStatisticsMetrics} - Network I/O statistics from /proc/net/dev</li>
 *   <li>{@link SystemMemoryMetrics} - JVM memory usage via OperatingSystemMXBean</li>
 * </ul>
 * <p>
 * These beans are automatically registered in the Spring context and bound to Micrometer's meter registry.
 * </p>
 * <h3>Example Configuration</h3>
 * <pre>{@code
 * // Enable/disable system metrics collection
 * microsphere.metrics.micrometer.system.enabled=true
 *
 * // Configure CGroup path (default: /sys/fs/cgroup/)
 * system.cgroup.dir=/custom/cgroup/path
 *
 * // Configure network stats file location
 * system.network.stats.file=/var/custom/net/dev
 * }</pre>
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
 * @see org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
 * @see org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.system.SystemMetricsAutoConfiguration
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMicrometerAvailable
@ConditionalOnProperty(name = SYSTEM_METRICS_ENABLED_PROPERTY_NAME, matchIfMissing = true)
@AutoConfigureAfter(name = {
        // Spring Boot Actuator API [2.0, 4.0)
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration",
        // Spring Boot Actuator API [4.0, )
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.system.SystemMetricsAutoConfiguration"
})
public class SystemMetricsAutoConfiguration {

    /**
     * The Property Name Prefix of System Metrics: "microsphere.metrics.micrometer.system."
     * <p>
     * All system-related configuration properties use this prefix.
     * </p>
     * <h3>Example Usage</h3>
     * <pre>{@code
     * String prefix = SystemMetricsAutoConfiguration.SYSTEM_METRICS_PROPERTY_NAME_PREFIX;
     * // Result: "microsphere.metrics.micrometer.system."
     * }</pre>
     */
    public static final String SYSTEM_METRICS_PROPERTY_NAME_PREFIX = PREFIX + "system" + DOT;

    /**
     * The Property Name of enabling System metrics: "microsphere.metrics.micrometer.system.enabled"
     * <p>
     * Set to {@code false} to disable automatic system metrics registration. Defaults to {@code true}.
     * </p>
     * <h3>Example Configuration</h3>
     * <pre>{@code
     * // application.properties or application.yml
     * microsphere.metrics.micrometer.system.enabled=false
     * }</pre>
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = "true",
            source = APPLICATION_SOURCE
    )
    public static final String SYSTEM_METRICS_ENABLED_PROPERTY_NAME = SYSTEM_METRICS_PROPERTY_NAME_PREFIX + ENABLED_PROPERTY_NAME;

    /**
     * The Placeholder of Network Stats File Location: "${system.network.stats.file:file:///proc/net/dev}"
     * <p>
     * Used for conditional bean creation based on resource availability.
     * If not configured, defaults to "/proc/net/dev".
     * </p>
     */
    public static final String NETWORK_STATS_FILE_LOCATION_PLACEHOLDER = "${" + NETWORK_STATS_FILE_PATH_PROPERTY_NAME + ":file://" + DEFAULT_NETWORK_STATS_FILE_PATH + "}";

    /**
     * The Property Placeholder of metrics collection interval: "${system.metrics.collection.interval:60000}"
     * <p>
     * Represents the default interval placeholder used when creating metrics beans.
     * Defaults to 60000 milliseconds (1 minute).
     * </p>
     */
    public static final String METRICS_COLLECTION_INTERVAL_PLACEHOLDER = "${" + METRICS_COLLECTION_INTERVAL_PROPERTY_NAME + ":" + DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE + "}";

    /**
     * Creates a {@link NetworkStatisticsMetrics} bean for collecting network I/O statistics.
     * <p>
     * This bean is conditionally created only if the configured network stats file exists.
     * It reads network device statistics from a file (typically /proc/net/dev) and exposes them as metrics.
     * </p>
     * <h3>Example Metrics Registered</h3>
     * <pre>{@code
     * system_network_receive_bytes       : Total bytes received by all network interfaces
     * system_network_transmit_bytes      : Total bytes transmitted by all network interfaces
     * system_network_receive_packets     : Total packets received
     * system_network_transmit_packets    : Total packets transmitted
     * }</pre>
     * <h3>Example Configuration</h3>
     * <pre>{@code
     * // Use default /proc/net/dev
     * system.network.stats.file=/proc/net/dev
     *
     * // Or customize path
     * system.network.stats.file=/var/metrics/net/dev
     * }</pre>
     *
     * @param networkStatsFileLocation the path to the network stats file (from property or default)
     * @param interval                 the metrics collection interval in milliseconds
     * @param resourceLoader           Spring's ResourceLoader for file access
     * @return new NetworkStatisticsMetrics instance
     * @throws IOException if the file cannot be accessed
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnResource(resources = NETWORK_STATS_FILE_LOCATION_PLACEHOLDER)
    public NetworkStatisticsMetrics networkStatisticsMetrics(
            @Value(NETWORK_STATS_FILE_LOCATION_PLACEHOLDER) String networkStatsFileLocation,
            @Value(METRICS_COLLECTION_INTERVAL_PLACEHOLDER) Duration interval,
            ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource(networkStatsFileLocation);
        File file = resource.getFile();
        return new NetworkStatisticsMetrics(file.toPath(), interval.toMillis());
    }

    /**
     * Creates a {@link SystemMemoryMetrics} bean for collecting JVM memory statistics.
     * <p>
     * This bean is conditionally created only if the OperatingSystemMXBean is available on the platform.
     * It exposes metrics for heap and non-heap memory usage.
     * </p>
     * <h3>Example Metrics Registered</h3>
     * <pre>{@code
     * system_memory_heap_used          : Current heap memory used
     * system_memory_heap_committed     : Heap memory committed
     * system_memory_heap_max           : Maximum heap memory
     * system_memory_non_heap_used      : Current non-heap memory used
     * }</pre>
     * <h3>Example Configuration</h3>
     * <pre>{@code
     * // No special configuration needed - uses OperatingSystemMXBean automatically
     * }</pre>
     *
     * @return new SystemMemoryMetrics instance
     * @see java.lang.management.ManagementFactory#getOperatingSystemMXBean()
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnPlatformMXBeanAvailable(type = OPERATING_SYSTEM_MXBEAN_CLASS_NAME)
    public SystemMemoryMetrics systemMemoryMetrics() {
        return new SystemMemoryMetrics();
    }
}