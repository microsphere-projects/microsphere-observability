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

import io.microsphere.logging.Logger;
import io.microsphere.observability.logging.log4j2.spring.boot.Log4j2KafkaAppenderProperties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static io.microsphere.logging.LoggerFactory.getLogger;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.springframework.kafka.test.utils.KafkaTestUtils.consumerProps;
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
                "microsphere.log4j2.kafka.appender.sync-send=true",
                "microsphere.log4j2.kafka.appender.send-event-timestamp=true",
                "microsphere.log4j2.kafka.appender.properties.bootstrap.servers=localhost:9092",
        }
)
@EmbeddedKafka(
        ports = 9092,
        topics = "${microsphere.log4j2.kafka.appender.topic}"
)
@DirtiesContext
@EnableAutoConfiguration
public class Log4j2AutoConfigurationIntegrationTest {

    private static final Logger logger = getLogger(Log4j2AutoConfigurationIntegrationTest.class);

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private Log4j2KafkaAppenderProperties properties;

    @Test
    void test() {
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
        assertEquals("microsphere-kafka-appender-default", kafkaProperties.get("client.id"));
        assertEquals("1000", kafkaProperties.get("batch.size"));

        for (int i = 0; i < 10; i++) {
            logger.trace("Hello, Log4j2!");
        }

        Map<String, Object> consumerProps = consumerProps("testGroup", "true", broker);
        consumerProps.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        ConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = cf.createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, true, properties.getTopic());

        ConsumerRecords<String, String> records = getRecords(consumer);

        assertFalse(records.isEmpty());

        records.forEach(record -> {
            assertEquals(this.properties.getKey(), record.key());
            assertEquals(this.properties.getTopic(), record.topic());
            assertNotNull(record);
            logger.trace(record.toString());
        });

        consumer.close();

    }
}
