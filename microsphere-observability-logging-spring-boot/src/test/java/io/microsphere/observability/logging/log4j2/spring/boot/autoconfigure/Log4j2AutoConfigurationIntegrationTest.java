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

package io.microsphere.observability.logging.log4j2.spring.boot.autoconfigure;

import io.microsphere.observability.logging.log4j2.DynamicLayout;
import io.microsphere.observability.logging.log4j2.spring.boot.Log4j2KafkaAppenderProperties;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

import static io.microsphere.collection.MapUtils.newHashMap;
import static io.microsphere.logging.LoggerUtils.trace;
import static io.microsphere.logging.log4j2.util.Log4j2Utils.getLoggerContext;
import static java.util.Collections.singleton;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.CLIENT_ID_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.logging.log4j.core.filter.CompositeFilter.createFilters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getRecords;

/**
 * {@link Log4j2AutoConfiguration} Integration Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Log4j2AutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                Log4j2AutoConfigurationIntegrationTest.class
        },
        webEnvironment = NONE,
        properties = {
                "spring.kafka.bootstrap-servers=localhost:9092",
                "microsphere.log4j2.kafka.appender.sync-send=true",
                "microsphere.log4j2.kafka.appender.send-event-timestamp=true"
        }
)
@EnableAutoConfiguration
public class Log4j2AutoConfigurationIntegrationTest {

//    @Autowired
//    private EmbeddedKafkaBroker broker;

    @Autowired
    private Log4j2AutoConfiguration.KafkaAppenderConfiguration kafkaAppenderConfiguration;

    @Autowired
    private Log4j2KafkaAppenderProperties properties;

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void test() {
        String bootstrapServers = properties.getProperties().get(BOOTSTRAP_SERVERS_CONFIG);

        assertTrue(this.properties.isEnabled());
        assertEquals("java-app-logs", this.properties.getTopic());
        assertEquals("java-app-logs-default", this.properties.getKey());
        assertNull(this.properties.getFilter());
        assertNull(this.properties.getLayout());
        assertEquals("%d{ISO8601} %p %t - %m", this.properties.getPatternLayout());
        assertEquals("microsphere-kafka-appender", this.properties.getName());
        assertTrue(this.properties.isIgnoreExceptions());
        assertEquals(3, this.properties.getRetryCount());
        assertTrue(this.properties.isSyncSend());
        assertTrue(this.properties.isSendEventTimestamp());

        Map<String, String> kafkaProperties = this.properties.getProperties();
        assertEquals("microsphere-kafka-appender-default", kafkaProperties.get(CLIENT_ID_CONFIG));
        assertEquals("1000", kafkaProperties.get("batch.size"));

        for (int i = 0; i < 10; i++) {
            trace(logger -> logger.trace("Hello, Log4j2!"));
        }

        // Map<String, Object> consumerProps = consumerProps("testGroup", "true", this.broker);
        Map<String, Object> consumerProps = newHashMap();
        consumerProps.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(GROUP_ID_CONFIG, "testGroup");
        consumerProps.put(ENABLE_AUTO_COMMIT_CONFIG, "true");
        consumerProps.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        // ConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        // Consumer<String, String> consumer = cf.createConsumer();
        // this.broker.consumeFromAnEmbeddedTopic(consumer, true, this.properties.getTopic());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);

        consumer.subscribe(singleton(this.properties.getTopic()));

        ConsumerRecords<String, String> records = getRecords(consumer);

        assertFalse(records.isEmpty());

        records.forEach(record -> {
            assertEquals(this.properties.getKey(), record.key());
            assertEquals(this.properties.getTopic(), record.topic());
            assertNotNull(record);
            trace(logger -> logger.trace(record.toString()));
        });

        consumer.close();
    }

    @Test
    void testGetter() {
        this.properties.setPatternLayout(null);

        String filterBeanName = "testFilter";
        this.properties.setFilter(filterBeanName);
        Filter filter = this.kafkaAppenderConfiguration.getFilter(this.context);
        assertNull(filter);

        ConfigurableListableBeanFactory beanFactory = this.context.getBeanFactory();
        CompositeFilter compositeFilter = createFilters(null);
        beanFactory.registerSingleton(filterBeanName, compositeFilter);
        filter = this.kafkaAppenderConfiguration.getFilter(this.context);
        assertSame(compositeFilter, filter);

        LoggerContext loggerContext = getLoggerContext();

        String layoutBeanName = "testLayout";
        this.properties.setLayout(layoutBeanName);
        Layout layout = this.kafkaAppenderConfiguration.getLayout(loggerContext, this.context);
        assertInstanceOf(DynamicLayout.class, layout);

        Layout newLayout = new DynamicLayout(loggerContext);
        beanFactory.registerSingleton(layoutBeanName, newLayout);
        layout = this.kafkaAppenderConfiguration.getLayout(loggerContext, this.context);
        assertSame(layout, newLayout);

        this.properties.setLayout(null);
        layout = this.kafkaAppenderConfiguration.getLayout(loggerContext, this.context);
        assertInstanceOf(DynamicLayout.class, layout);

        this.properties.setPatternLayout("%m");
        layout = this.kafkaAppenderConfiguration.getLayout(loggerContext, this.context);
        assertFalse(layout instanceof DynamicLayout);
    }
}
