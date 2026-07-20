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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
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

import java.util.List;
import java.util.Map;

import static io.microsphere.logging.LoggerFactory.getLogger;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.springframework.kafka.test.utils.KafkaTestUtils.consumerProps;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getRecords;

/**
 * {@link KafkaMetricsAutoConfiguration} Integration Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see KafkaMetricsAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                KafkaMetricsAutoConfigurationIntegrationTest.class
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
public class KafkaMetricsAutoConfigurationIntegrationTest {

    private static final Logger logger = getLogger(KafkaMetricsAutoConfigurationIntegrationTest.class);

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private Log4j2KafkaAppenderProperties properties;

    @Autowired
    private MeterRegistry registry;

    @Test
    void test() {

        Map<String, Object> consumerProps = consumerProps("testGroup", "true", this.broker);
        consumerProps.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        ConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = cf.createConsumer();
        this.broker.consumeFromAnEmbeddedTopic(consumer, true, this.properties.getTopic());

        for (int i = 0; i < 10; i++) {
            logger.trace("Testing {}", i + 1);
        }

        ConsumerRecords<String, String> records = getRecords(consumer);

        assertFalse(records.isEmpty());

        consumer.close();

        List<Meter> meters = registry.getMeters();

        long count = meters.stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .filter(name -> name.startsWith("kafka."))
                .count();

        assertTrue(count > 0);
    }
}
