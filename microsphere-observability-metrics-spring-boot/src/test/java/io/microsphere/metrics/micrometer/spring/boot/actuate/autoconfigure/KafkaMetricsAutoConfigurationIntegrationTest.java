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
import io.microsphere.observability.logging.log4j2.spring.boot.Log4j2KafkaAppenderProperties;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static io.microsphere.collection.MapUtils.newHashMap;
import static io.microsphere.logging.LoggerUtils.trace;
import static java.util.Collections.singleton;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
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
//@EmbeddedKafka(
//        ports = 9092,
//        topics = "${microsphere.log4j2.kafka.appender.topic}"
//)
@DirtiesContext
@EnableAutoConfiguration
class KafkaMetricsAutoConfigurationIntegrationTest {

//    @Autowired
//    private EmbeddedKafkaBroker broker;

    @Autowired
    private Log4j2KafkaAppenderProperties properties;

    @Autowired
    private MeterRegistry registry;

    @Test
    void test() {
        String bootstrapServers = properties.getProperties().get(BOOTSTRAP_SERVERS_CONFIG);

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

        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            trace(logger -> logger.trace("Testing {}", index));
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
