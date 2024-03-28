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
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.microsphere.logging.log4j2.spring.boot.Log4j2KafkaAppenderProperties;
import io.microsphere.logging.log4j2.util.Log4j2Utils;
import io.microsphere.metrics.micrometer.instrument.binder.sentinel.SentinelMetrics;
import io.microsphere.metrics.micrometer.instrument.binder.system.CGroupMemoryMetrics;
import io.microsphere.metrics.micrometer.instrument.binder.system.NetworkStatisticsMetrics;
import io.microsphere.metrics.micrometer.instrument.binder.system.SystemMemoryMetrics;
import io.microsphere.metrics.micrometer.prometheus.client.sentinel.SentinelCollector;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnCGroup;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnEnabledPrometheusMetricsExport;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnEnabledPrometheusPushGateway;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled;
import io.microsphere.sentinel.spring.boot.condition.ConditionalOnSentinelEnabled;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.BasicAuthHttpConnectionFactory;
import io.prometheus.client.exporter.PushGateway;
import org.apache.kafka.clients.producer.Producer;
import org.apache.logging.log4j.core.appender.mom.kafka.KafkaAppender;
import org.apache.logging.log4j.core.appender.mom.kafka.KafkaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import static io.microsphere.constants.PropertyConstants.ENABLED_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled.PREFIX;
import static io.microsphere.metrics.micrometer.util.MicrometerUtils.getScheduledExecutor;
import static io.microsphere.reflect.FieldUtils.getFieldValue;
import static io.microsphere.spring.boot.actuate.autoconfigure.ActuatorAutoConfiguration.ACTUATOR_TASK_SCHEDULER_SERVICE_BEAN_NAME;

/**
 * Micrometer(Metrics) Auto-Configuration for Actuator
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see MeterRegistry
 * @since 1.0.0
 */
@ConditionalOnMicrometerEnabled
@Import(value = {
        MicrometerAutoConfiguration.SystemConfiguration.class,
        MicrometerAutoConfiguration.CGGroupConfiguration.class,
        MicrometerAutoConfiguration.JvmConfiguration.class,
        MicrometerAutoConfiguration.SentinelMetricsConfiguration.class,
        MicrometerAutoConfiguration.KafkaMetricsConfiguration.class,
        MicrometerAutoConfiguration.PrometheusMetricsConfiguration.class,
})
public class MicrometerAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MicrometerAutoConfiguration.class);

    @ConditionalOnProperty(prefix = PREFIX + "system", name = ENABLED_PROPERTY_NAME, matchIfMissing = true)
    static class SystemConfiguration {

        @Bean
        public NetworkStatisticsMetrics networkStatisticsMetrics(@Value("${microsphere.metrics.collection.interval:60000}") Duration interval) {
            return new NetworkStatisticsMetrics(getScheduledExecutor(), interval.toMillis());
        }

        @Bean
        @ConditionalOnMissingBean
        public SystemMemoryMetrics systemMemoryMetrics() {
            return new SystemMemoryMetrics();
        }
    }

    @ConditionalOnProperty(prefix = PREFIX + "cgroup", name = ENABLED_PROPERTY_NAME, matchIfMissing = true)
    @ConditionalOnCGroup
    static class CGGroupConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public CGroupMemoryMetrics cGroupMemoryMetrics() {
            return new CGroupMemoryMetrics();
        }
    }

    @ConditionalOnProperty(prefix = PREFIX + "jvm", name = ENABLED_PROPERTY_NAME, matchIfMissing = true)
    static class JvmConfiguration {

        @Value("${microsphere.executor-service.metrics.prefix:}")
        private String executorServiceMetricsPrefix;

        @Value("${microsphere.executor-service.metrics.tags:}")
        private String[] executorServiceMetricsTags = new String[0];

        @Bean
        @ConditionalOnMissingBean
        public JvmCompilationMetrics jvmCompilationMetrics() {
            return new JvmCompilationMetrics();
        }

        @Bean
        @ConditionalOnMissingBean
        public JvmInfoMetrics jvmInfoMetrics() {
            return new JvmInfoMetrics();
        }

        @EventListener(ApplicationStartedEvent.class)
        public void registerExecutorServiceMetrics(ApplicationStartedEvent event) {
            ApplicationContext context = event.getApplicationContext();
            MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
            registerExecutorServiceMetrics(context, ConcurrentTaskExecutor.class, ConcurrentTaskExecutor::getConcurrentExecutor, meterRegistry);
            registerExecutorServiceMetrics(context, ExecutorService.class, e -> e, meterRegistry);
            registerExecutorServiceMetrics(ForkJoinPool.commonPool(), "ForkJoinPool-commonPool", meterRegistry);
        }

        private <T> void registerExecutorServiceMetrics(ApplicationContext applicationContext, Class<T> beanType,
                                                        Function<T, Executor> executorConverter, MeterRegistry meterRegistry) {
            Map<String, T> beansMap = applicationContext.getBeansOfType(beanType);
            if (beansMap.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No Bean can't be found in the ApplicationContext[id: '{}'] by type : '{}'",
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
            if (logger.isDebugEnabled()) {
                logger.debug("ExecutorService[name: '{}', type: '{}'] {} -> ExecutorServiceMetrics[prefix: '{}', tags: {}]",
                        name, executorService.getClass().getName(), prefix, Arrays.asList(executorServiceMetricsTags));
            }
            return new ExecutorServiceMetrics(executorService, name, prefix, Tags.of(executorServiceMetricsTags));
        }
    }

    @ConditionalOnProperty(prefix = PREFIX + "sentinel", name = ENABLED_PROPERTY_NAME, matchIfMissing = true)
    @ConditionalOnSentinelEnabled
    static class SentinelMetricsConfiguration {

        @Bean
        @ConditionalOnBean(type = "io.micrometer.prometheus.PrometheusMeterRegistry")
        public SentinelCollector sentinelCollector(PrometheusMeterRegistry registry,
                                                   @Value("${microsphere.metrics.collection.interval:60000}") Duration interval,
                                                   @Value("${spring.application.name:default}") String applicationName) {
            SentinelCollector sentinelCollector = new SentinelCollector(interval.toMillis());
            sentinelCollector.commonLabel("application", applicationName);
            CollectorRegistry collectorRegistry = registry.getPrometheusRegistry();
            sentinelCollector.register(collectorRegistry);
            return sentinelCollector;
        }

        @Bean
        @ConditionalOnMissingBean(SentinelCollector.class)
        public SentinelMetrics sentinelMetrics() {
            return new SentinelMetrics();
        }

    }

    @ConditionalOnProperty(prefix = PREFIX + "kafka", name = ENABLED_PROPERTY_NAME, matchIfMissing = true)
    @ConditionalOnClass(name = "org.apache.kafka.clients.KafkaClient")
    static class KafkaMetricsConfiguration {

        @Bean
        @ConditionalOnBean(Log4j2KafkaAppenderProperties.class)
        public ApplicationListener<ApplicationReadyEvent> applicationReadyEventApplicationListener(Log4j2KafkaAppenderProperties properties) {
            return event -> {
                bindKafkaAppenderMetrics(event, properties);
            };
        }

        private void bindKafkaAppenderMetrics(ApplicationReadyEvent event, Log4j2KafkaAppenderProperties properties) {
            Producer producer = getKafkaProducer(properties);
            if (producer == null) {
                return;
            }
            ConfigurableApplicationContext context = event.getApplicationContext();
            String clientId = properties.getProperties().get("client.id");
            // Keep the same behavior of org.springframework.kafka.core.MicrometerProducerListener
            Iterable<Tag> tags = Arrays.asList(Tag.of("spring.id", clientId));
            KafkaClientMetrics kafkaClientMetrics = new KafkaClientMetrics(producer, tags);
            MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
            kafkaClientMetrics.bindTo(meterRegistry);
        }

        private Producer getKafkaProducer(Log4j2KafkaAppenderProperties properties) {
            String loggerName = properties.getName();
            KafkaAppender kafkaAppender = Log4j2Utils.findAppender(loggerName);
            Producer producer = null;
            if (kafkaAppender != null) {
                KafkaManager kafkaManager = getFieldValue(kafkaAppender, "manager", KafkaManager.class);
                if (kafkaManager != null) {
                    producer = getFieldValue(kafkaManager, "producer", Producer.class);
                }
            }
            return producer;
        }
    }

    @ConditionalOnProperty(prefix = PREFIX + "prometheus", name = ENABLED_PROPERTY_NAME, matchIfMissing = true)
    @ConditionalOnEnabledPrometheusMetricsExport
    static class PrometheusMetricsConfiguration {

        @Bean
        @ConditionalOnEnabledPrometheusPushGateway
        public PrometheusPushGatewayManager prometheusPushGatewayManager(CollectorRegistry collectorRegistry,
                                                                         PrometheusProperties prometheusProperties,
                                                                         @Autowired @Qualifier(ACTUATOR_TASK_SCHEDULER_SERVICE_BEAN_NAME) ThreadPoolTaskScheduler threadPoolTaskScheduler) {
            PrometheusProperties.Pushgateway properties = prometheusProperties.getPushgateway();
            Duration pushRate = properties.getPushRate();
            String job = properties.getJob();
            Map<String, String> groupingKey = properties.getGroupingKey();
            PrometheusPushGatewayManager.ShutdownOperation shutdownOperation = properties.getShutdownOperation();
            PushGateway pushGateway = initializePushGateway(properties.getBaseUrl());
            if (StringUtils.hasText(properties.getUsername())) {
                pushGateway.setConnectionFactory(new BasicAuthHttpConnectionFactory(properties.getUsername(), properties.getPassword()));
            }
            return new PrometheusPushGatewayManager(pushGateway, collectorRegistry, threadPoolTaskScheduler,
                    pushRate, job, groupingKey, shutdownOperation);
        }

        private PushGateway initializePushGateway(String url) {
            try {
                return new PushGateway(new URL(url));
            } catch (MalformedURLException ex) {
                logger.warn("Invalid PushGateway base url '{}': update your configuration to a valid URL", url);
                return new PushGateway(url);
            }
        }

    }
}
