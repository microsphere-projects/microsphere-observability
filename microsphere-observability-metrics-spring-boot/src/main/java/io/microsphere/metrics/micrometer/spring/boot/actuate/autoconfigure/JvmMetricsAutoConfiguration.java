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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.microsphere.annotation.ConfigurationProperty;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerAvailable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import static io.micrometer.core.instrument.Tags.of;
import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;
import static io.microsphere.constants.PropertyConstants.ENABLED_PROPERTY_NAME;
import static io.microsphere.constants.SymbolConstants.DOT;
import static io.microsphere.logging.LoggerUtils.trace;
import static io.microsphere.logging.LoggerUtils.warn;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.JvmMetricsAutoConfiguration.JVM_METRICS_ENABLED_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled.PREFIX;
import static io.microsphere.util.ArrayUtils.EMPTY_STRING_ARRAY;
import static io.microsphere.util.ArrayUtils.arrayToString;
import static java.util.concurrent.ForkJoinPool.commonPool;

/**
 * The Auto-Configuration class for JVM Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
 * @see org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
 * @see org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMicrometerAvailable
@ConditionalOnProperty(name = JVM_METRICS_ENABLED_PROPERTY_NAME, matchIfMissing = true)
@ConditionalOnClass(name = {
        "io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics"                                    // Micrometer Core API
})
@AutoConfigureAfter(name = {
        // Spring Boot Actuator API [2.0, 4.0)
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration",
        // Spring Boot Actuator API [4.0, )
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration"

})
public class JvmMetricsAutoConfiguration {

    /**
     * The Property Name prefix of JVM metrics collection: "microsphere.metrics.micrometer.jvm."
     * <p>
     * All JVM-related configuration properties start with this prefix.
     * </p>
     * <h3>Example Usage</h3>
     * <pre>{@code
     * String prefix = JvmMetricsAutoConfiguration.JVM_METRICS_PROPERTY_NAME_PREFIX;
     * // Result: "microsphere.metrics.micrometer.jvm."
     * }</pre>
     */
    public static final String JVM_METRICS_PROPERTY_NAME_PREFIX = PREFIX + "jvm" + DOT;

    /**
     * The Property Name prefix of {@link ExecutorService} Metrics: "microsphere.metrics.micrometer.jvm.executor-service."
     * <p>
     * Used to configure all ExecutorService metric properties including prefix and tags.
     * </p>
     * <h3>Example Usage</h3>
     * <pre>{@code
     * String executorPrefix = JvmMetricsAutoConfiguration.EXECUTOR_SERVICE_METRICS_PROPERTY_NAME_PREFIX;
     * // Result: "microsphere.metrics.micrometer.jvm.executor-service."
     * }</pre>
     */
    public static final String EXECUTOR_SERVICE_METRICS_PROPERTY_NAME_PREFIX = JVM_METRICS_PROPERTY_NAME_PREFIX + "executor-service" + DOT;

    /**
     * The Property Name of enabling JVM metrics: "microsphere.metrics.micrometer.jvm.enabled"
     * <p>
     * Set to {@code false} to disable automatic JVM metrics registration. Defaults to {@code true}.
     * </p>
     * <h3>Example Configuration</h3>
     * <pre>{@code
     * // application.properties or application.yml
     * microsphere.metrics.micrometer.jvm.enabled=false
     * }</pre>
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = "true",
            source = APPLICATION_SOURCE
    )
    public static final String JVM_METRICS_ENABLED_PROPERTY_NAME = JVM_METRICS_PROPERTY_NAME_PREFIX + ENABLED_PROPERTY_NAME;

    /**
     * The Property Name of {@link ExecutorService} Metrics prefix: "microsphere.metrics.micrometer.jvm.executor-service.prefix"
     * <p>
     * Configures the base prefix for ExecutorService metrics. If not set, uses Micrometer's default naming.
     * </p>
     * <h3>Example Configuration</h3>
     * <pre>{@code
     * // application.properties
     * microsphere.metrics.micrometer.jvm.executor-service.prefix=tomcat-executors
     * }</pre>
     */
    @ConfigurationProperty(
            source = APPLICATION_SOURCE
    )
    public static final String EXECUTOR_SERVICE_METRICS_PREFIX_PROPERTY_NAME = EXECUTOR_SERVICE_METRICS_PROPERTY_NAME_PREFIX + "prefix";

    /**
     * The Property Name of {@link ExecutorService} Metrics tags: "microsphere.metrics.micrometer.jvm.executor-service.tags"
     * <p>
     * Comma-separated list of key=value pairs to attach as common tags to ExecutorService metrics.
     * </p>
     * <h3>Example Configuration</h3>
     * <pre>{@code
     * // application.properties
     * microsphere.metrics.micrometer.jvm.executor-service.tags=service=user-service,environment=production
     * }</pre>
     */
    @ConfigurationProperty(
            source = APPLICATION_SOURCE
    )
    public static final String EXECUTOR_SERVICE_METRICS_TAGS_PROPERTY_NAME = EXECUTOR_SERVICE_METRICS_PROPERTY_NAME_PREFIX + "tags";

    /**
     * Initializes the executor service metrics prefix configuration value.
     * <p>
     * This field is populated from the Spring property {@link #EXECUTOR_SERVICE_METRICS_PREFIX_PROPERTY_NAME}.
     * </p>
     */
    @Value("${" + EXECUTOR_SERVICE_METRICS_PREFIX_PROPERTY_NAME + ":}")
    private String executorServiceMetricsPrefix;

    /**
     * Initializes the executor service metrics tags configuration value.
     * <p>
     * This field is populated from the Spring property {@link #EXECUTOR_SERVICE_METRICS_TAGS_PROPERTY_NAME}.
     * Default value is an empty array if no tags are configured.
     * </p>
     */
    @Value("${" + EXECUTOR_SERVICE_METRICS_TAGS_PROPERTY_NAME + ":}")
    private String[] executorServiceMetricsTags = EMPTY_STRING_ARRAY;

    /**
     * Registers metrics for all ExecutorService beans found in the application context when the application starts.
     * <p>
     * This method automatically discovers and binds metrics for:
     * </p>
     * <ul>
     *   <li>{@link ConcurrentTaskExecutor} instances (Spring's task executors)</li>
     *   <li>{@link ScheduledExecutorService} beans</li>
     *   <li>{@link ExecutorService} beans</li>
     *   <li>{@link Executor} beans</li>
     *   <li>{@link ForkJoinPool#commonPool()} - common thread pool</li>
     * </ul>
     * <h3>Example Behavior</h3>
     * <pre>{@code
     * // When Spring Boot application starts, automatically registers metrics like:
     * // - tomcat-api-executor_threads_active
     * // - myScheduledExecutor_pool_size
     * // - ForkJoinPool-commonPool_active_count
     * }</pre>
     *
     * @param event the application started event used to trigger registration
     */
    @EventListener(ApplicationStartedEvent.class)
    public void registerExecutorServiceMetrics(ApplicationStartedEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();
        MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
        registerExecutorServiceMetrics(context, ConcurrentTaskExecutor.class, ConcurrentTaskExecutor::getConcurrentExecutor, meterRegistry);
        registerExecutorServiceMetrics(context, ScheduledExecutorService.class, e -> e, meterRegistry);
        registerExecutorServiceMetrics(context, ExecutorService.class, e -> e, meterRegistry);
        registerExecutorServiceMetrics(context, Executor.class, e -> e, meterRegistry);
        registerExecutorServiceMetrics(commonPool(), "ForkJoinPool-commonPool", meterRegistry);
    }

    /**
     * Registers ExecutorService metrics from discovered beans in the application context.
     * <p>
     * This internal method searches for beans of the specified type, converts them to {@link Executor} instances,
     * and binds their metrics to the provided meter registry if they are {@link ExecutorService} instances.
     * </p>
     * <p>
     * Skips logging warnings for empty contexts (trace level) and warns when executors don't implement ExecutorService.
     * </p>
     * <h3>Example Usage</h3>
     * <pre>{@code
     * // Automatically called during application startup
     * // Searches for ScheduledExecutorService beans and binds metrics
     * }</pre>
     *
     * @param context           the Spring application context to search for beans
     * @param beanType          the type of beans to search for (e.g., {@code ScheduledExecutorService.class})
     * @param executorConverter function to extract or convert bean to {@link Executor} instance
     * @param meterRegistry     the Micrometer meter registry to bind metrics to
     * @param <T>               the type of beans being searched
     */
    private <T> void registerExecutorServiceMetrics(ConfigurableApplicationContext context, Class<T> beanType,
                                                    Function<T, Executor> executorConverter, MeterRegistry meterRegistry) {
        Map<String, T> beansMap = context.getBeansOfType(beanType);
        if (beansMap.isEmpty()) {
            trace(logger -> logger.trace("No Bean can't be found in the ApplicationContext[id: '{}'] by type : '{}'",
                    context.getId(), beanType.getName()));
            return;
        }
        for (Map.Entry<String, T> beanEntry : beansMap.entrySet()) {
            String beanName = beanEntry.getKey();
            T bean = beanEntry.getValue();
            Executor executor = executorConverter.apply(bean);
            if (executor instanceof ExecutorService) {
                registerExecutorServiceMetrics((ExecutorService) executor, beanName, meterRegistry);
            } else {
                warn(logger -> logger.warn("Spring Bean[name: '{}', type: '{}'] associating Executor[type: '{}'] is not a instance of ExecutorService",
                        beanName, beanType.getName(), executor.getClass().getName()));
            }
        }
    }

    /**
     * Binds ExecutorService metrics to the provided meter registry.
     * <p>
     * Creates an {@link ExecutorServiceMetrics} binder instance and registers it with the given registry.
     * </p>
     * <h3>Example Metrics Registered</h3>
     * <pre>{@code
     * - threads_active      : Number of threads currently active
     * - threads_daemon      : Number of daemon threads
     * - threads_live        : Total number of live threads
     * - pool_sizes          : Size of the thread pool
     * }</pre>
     *
     * @param executorService  the ExecutorService instance to bind metrics for
     * @param name             the identifier/name for this ExecutorService
     * @param meterRegistry    the Micrometer meter registry to bind metrics to
     */
    private <T> void registerExecutorServiceMetrics(ExecutorService executorService, String name, MeterRegistry meterRegistry) {
        ExecutorServiceMetrics executorServiceMetrics = createExecutorServiceMetrics(executorService, name);
        executorServiceMetrics.bindTo(meterRegistry);
    }

    /**
     * Creates an {@link ExecutorServiceMetrics} binder instance for the given ExecutorService.
     * <p>
     * Applies the configured prefix and tags to the metrics if they are set in the properties.
     * Logs trace information about the created metrics binder.
     * </p>
     * <h3>Example Usage</h3>
     * <pre>{@code
     * ExecutorServiceMetrics metricsBinder = createExecutorServiceMetrics(executorService, "myPool");
     * metricsBinder.bindTo(meterRegistry);
     * // Result: Creates metrics like mypool_threads_active with configured tags
     * }</pre>
     *
     * @param executorService the ExecutorService instance to create metrics for
     * @param name            the identifier/name for this ExecutorService
     * @return new ExecutorServiceMetrics binder instance with configured prefix and tags
     */
    private ExecutorServiceMetrics createExecutorServiceMetrics(ExecutorService executorService, String name) {
        String prefix = executorServiceMetricsPrefix;
        trace(logger -> logger.trace("ExecutorService[name: '{}', type: '{}'] {} -> ExecutorServiceMetrics[prefix: '{}', tags: {}]",
                name, executorService.getClass().getName(), prefix, arrayToString(executorServiceMetricsTags)));
        return new ExecutorServiceMetrics(executorService, name, prefix, of(executorServiceMetricsTags));
    }
}