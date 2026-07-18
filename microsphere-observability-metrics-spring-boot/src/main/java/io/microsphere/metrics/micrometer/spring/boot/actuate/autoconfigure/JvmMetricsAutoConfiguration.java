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
import io.microsphere.logging.Logger;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerAvailable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static io.micrometer.core.instrument.Tags.of;
import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;
import static io.microsphere.collection.Lists.ofList;
import static io.microsphere.constants.PropertyConstants.ENABLED_PROPERTY_NAME;
import static io.microsphere.constants.SymbolConstants.DOT;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.JvmMetricsAutoConfiguration.JVM_METRICS_ENABLED_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled.PREFIX;
import static io.microsphere.util.ArrayUtils.EMPTY_STRING_ARRAY;
import static java.util.concurrent.ForkJoinPool.commonPool;

/**
 * The Auto-Configuration class for JVM Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMicrometerAvailable
@ConditionalOnProperty(name = JVM_METRICS_ENABLED_PROPERTY_NAME, matchIfMissing = true)
@ConditionalOnClass(name = {
        "io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics"                               // Micrometer API
})
@AutoConfigureAfter(name = {
        "org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration",           // Spring Boot API [2.0, 4.0)
        "org.springframework.boot.micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration"     // Spring Boot API [4.0, )
})
public class JvmMetricsAutoConfiguration {

    private static final Logger logger = getLogger(JvmMetricsAutoConfiguration.class);

    /**
     * The Property Name prefix of JVM : "microsphere.metrics.micrometer.jvm."
     */
    public static final String JVM_METRICS_PROPERTY_NAME_PREFIX = PREFIX + "jvm" + DOT;

    /**
     * The Property Name prefix of {@link ExecutorService} Metrics : "microsphere.metrics.micrometer.jvm.executor-service."
     */
    public static final String EXECUTOR_SERVICE_METRICS_PROPERTY_NAME_PREFIX = JVM_METRICS_PROPERTY_NAME_PREFIX + "executor-service" + DOT;

    /**
     * The Property Name of enabling JVM metrics : "microsphere.metrics.micrometer.jvm.enabled"
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = "true",
            source = APPLICATION_SOURCE
    )
    public static final String JVM_METRICS_ENABLED_PROPERTY_NAME = JVM_METRICS_PROPERTY_NAME_PREFIX + ENABLED_PROPERTY_NAME;

    /**
     * The Property Name of {@link ExecutorService} Metrics prefix : "microsphere.metrics.micrometer.jvm.executor-service.prefix"
     */
    @ConfigurationProperty(
            source = APPLICATION_SOURCE
    )
    public static final String EXECUTOR_SERVICE_METRICS_PREFIX_PROPERTY_NAME = EXECUTOR_SERVICE_METRICS_PROPERTY_NAME_PREFIX + "prefix";

    /**
     * The Property Name of {@link ExecutorService} Metrics tags : "microsphere.metrics.micrometer.jvm.executor-service.tags"
     */
    @ConfigurationProperty(
            source = APPLICATION_SOURCE
    )
    public static final String EXECUTOR_SERVICE_METRICS_TAGS_PROPERTY_NAME = EXECUTOR_SERVICE_METRICS_PROPERTY_NAME_PREFIX + "tags";

    @Value("${" + EXECUTOR_SERVICE_METRICS_PREFIX_PROPERTY_NAME + ":}")
    private String executorServiceMetricsPrefix;

    @Value("${" + EXECUTOR_SERVICE_METRICS_TAGS_PROPERTY_NAME + ":}")
    private String[] executorServiceMetricsTags = EMPTY_STRING_ARRAY;

    @EventListener(ApplicationStartedEvent.class)
    public void registerExecutorServiceMetrics(ApplicationStartedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
        registerExecutorServiceMetrics(context, ConcurrentTaskExecutor.class, ConcurrentTaskExecutor::getConcurrentExecutor, meterRegistry);
        registerExecutorServiceMetrics(context, ExecutorService.class, e -> e, meterRegistry);
        registerExecutorServiceMetrics(commonPool(), "ForkJoinPool-commonPool", meterRegistry);
    }

    private <T> void registerExecutorServiceMetrics(ApplicationContext applicationContext, Class<T> beanType,
                                                    Function<T, Executor> executorConverter, MeterRegistry meterRegistry) {
        Map<String, T> beansMap = applicationContext.getBeansOfType(beanType);
        if (beansMap.isEmpty()) {
            if (logger.isTraceEnabled()) {
                logger.trace("No Bean can't be found in the ApplicationContext[id: '{}'] by type : '{}'",
                        applicationContext.getId(), beanType.getName());
            }
            return;
        }
        for (Map.Entry<String, T> beanEntry : beansMap.entrySet()) {
            String beanName = beanEntry.getKey();
            T bean = beanEntry.getValue();
            Executor executor = executorConverter.apply(bean);
            if (executor instanceof ExecutorService) {
                registerExecutorServiceMetrics((ExecutorService) executor, beanName, meterRegistry);
            } else {
                logger.warn("Spring Bean[name: '{}', type: '{}'] associating Executor[type: '{}'] is not a instance of ExecutorService",
                        beanName, beanType.getName(), executor.getClass().getName());
            }
        }
    }

    private <T> void registerExecutorServiceMetrics(ExecutorService executorService, String name, MeterRegistry meterRegistry) {
        ExecutorServiceMetrics executorServiceMetrics = createExecutorServiceMetrics(executorService, name);
        executorServiceMetrics.bindTo(meterRegistry);
    }

    private ExecutorServiceMetrics createExecutorServiceMetrics(ExecutorService executorService, String name) {
        String prefix = executorServiceMetricsPrefix;
        if (logger.isTraceEnabled()) {
            logger.trace("ExecutorService[name: '{}', type: '{}'] {} -> ExecutorServiceMetrics[prefix: '{}', tags: {}]",
                    name, executorService.getClass().getName(), prefix, ofList(executorServiceMetricsTags));
        }
        return new ExecutorServiceMetrics(executorService, name, prefix, of(executorServiceMetricsTags));
    }
}