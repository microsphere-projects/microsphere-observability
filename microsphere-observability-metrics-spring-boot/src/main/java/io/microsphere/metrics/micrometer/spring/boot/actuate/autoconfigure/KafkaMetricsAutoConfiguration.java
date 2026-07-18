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
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.microsphere.annotation.ConfigurationProperty;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerAvailable;
import io.microsphere.observability.logging.log4j2.spring.boot.Log4j2KafkaAppenderProperties;
import org.apache.kafka.clients.producer.Producer;
import org.apache.logging.log4j.core.appender.mom.kafka.KafkaAppender;
import org.apache.logging.log4j.core.appender.mom.kafka.KafkaManager;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.micrometer.core.instrument.Tag.of;
import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;
import static io.microsphere.collection.Lists.ofList;
import static io.microsphere.constants.PropertyConstants.ENABLED_PROPERTY_NAME;
import static io.microsphere.constants.SymbolConstants.DOT;
import static io.microsphere.logging.log4j2.util.Log4j2Utils.findAppender;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.KafkaMetricsAutoConfiguration.KAFKA_METRICS_ENABLED_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled.PREFIX;
import static io.microsphere.reflect.FieldUtils.getFieldValue;

/**
 * The Auto-Configuration class for Apache Kafka Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration
 * @see org.springframework.boot.kafka.autoconfigure.metrics.KafkaMetricsAutoConfiguration
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = KAFKA_METRICS_ENABLED_PROPERTY_NAME, matchIfMissing = true)
@ConditionalOnMicrometerAvailable
@ConditionalOnClass(name = {
        "io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics",                                   // Micrometer Core API
        "org.apache.kafka.clients.KafkaClient",                                                            // Kafka Client API
        "org.springframework.kafka.core.ProducerFactory",                                                  // Spring Kafka API
        "io.microsphere.observability.logging.log4j2.spring.boot.Log4j2KafkaAppenderProperties"            // Microsphere Observability Logging Spring Boot API
})
@AutoConfigureAfter(name = {
        // Spring Boot Actuator API [2.0, 4.0)
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration",
        // Spring Boot Actuator API [4.0, )
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.kafka.autoconfigure.metrics.KafkaMetricsAutoConfiguration"

})
public class KafkaMetricsAutoConfiguration {

    /**
     * The Property Name of enabling Apache Kafka metrics : "microsphere.metrics.micrometer.kafka.enabled"
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = "true",
            source = APPLICATION_SOURCE
    )
    public static final String KAFKA_METRICS_ENABLED_PROPERTY_NAME = PREFIX + "kafka" + DOT + ENABLED_PROPERTY_NAME;

    @Bean
    @ConditionalOnBean(Log4j2KafkaAppenderProperties.class)
    public ApplicationListener<ApplicationStartedEvent> applicationReadyEventApplicationListener(Log4j2KafkaAppenderProperties properties) {
        return event -> bindKafkaAppenderMetrics(event, properties);
    }

    private void bindKafkaAppenderMetrics(ApplicationStartedEvent event, Log4j2KafkaAppenderProperties properties) {
        Producer producer = getKafkaProducer(properties);
        if (producer == null) {
            return;
        }
        ConfigurableApplicationContext context = event.getApplicationContext();
        String clientId = properties.getProperties().get("client.id");
        // Keep the same behavior of org.springframework.kafka.core.MicrometerProducerListener
        Iterable<Tag> tags = ofList(of("spring.id", clientId));
        KafkaClientMetrics kafkaClientMetrics = new KafkaClientMetrics(producer, tags);
        MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
        kafkaClientMetrics.bindTo(meterRegistry);
    }

    private Producer getKafkaProducer(Log4j2KafkaAppenderProperties properties) {
        String loggerName = properties.getName();
        KafkaAppender kafkaAppender = findAppender(loggerName);
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
