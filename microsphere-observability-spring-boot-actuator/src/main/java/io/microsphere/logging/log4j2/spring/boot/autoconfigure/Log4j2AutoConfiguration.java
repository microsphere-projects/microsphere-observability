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
package io.microsphere.logging.log4j2.spring.boot.autoconfigure;

import io.microsphere.logging.log4j2.DefaultKafkaLayout;
import io.microsphere.logging.log4j2.appender.InMemoryAppender;
import io.microsphere.logging.log4j2.spring.boot.listener.AddingInMemoryAppenderListener;
import io.microsphere.logging.log4j2.spring.boot.Log4j2KafkaAppenderProperties;
import io.microsphere.logging.log4j2.util.Log4j2Utils;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.mom.kafka.KafkaAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import java.io.Serializable;
import java.util.Map;

import static io.microsphere.logging.log4j2.appender.InMemoryAppender.findInMemoryAppender;
import static io.microsphere.logging.log4j2.spring.boot.Log4j2KafkaAppenderProperties.PREFIX;
import static io.microsphere.spring.util.BeanUtils.getBeanIfAvailable;
import static org.apache.logging.log4j.core.appender.mom.kafka.KafkaAppender.newBuilder;
import static org.apache.logging.log4j.core.config.Property.createProperty;
import static org.springframework.util.StringUtils.hasText;

/**
 * Log4j2 Auto-Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Log4J2LoggingSystem
 * @see LoggingApplicationListener
 * @see KafkaAppender
 * @see KafkaAutoConfiguration
 * @since 1.0.0
 */
@ConditionalOnClass(name = "org.apache.logging.log4j.core.impl.Log4jContextFactory")
@Import(value = {
        Log4j2AutoConfiguration.KafkaAppenderConfiguration.class
})
public class Log4j2AutoConfiguration {

    /**
     * The configuration class for {@link KafkaAppender}
     *
     * @see AddingInMemoryAppenderListener
     */
    @ConditionalOnClass(name = "org.apache.kafka.clients.KafkaClient")
    @ConditionalOnProperty(prefix = PREFIX, name = "properties.bootstrap.servers")
    @EnableConfigurationProperties(Log4j2KafkaAppenderProperties.class)
    class KafkaAppenderConfiguration {

        private final Log4j2KafkaAppenderProperties kafkaAppenderProperties;

        private ConfigurableApplicationContext context;

        public KafkaAppenderConfiguration(Log4j2KafkaAppenderProperties kafkaAppenderProperties) {
            this.kafkaAppenderProperties = kafkaAppenderProperties;
        }

        @EventListener(ApplicationPreparedEvent.class)
        public void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
            this.context = event.getApplicationContext();
            initializeKafkaAppender();
        }

        private void initializeKafkaAppender() {
            KafkaAppender kafkaAppender = buildKafkaAppender();
            InMemoryAppender inMemoryAppender = findInMemoryAppender();
            inMemoryAppender.transfer(kafkaAppender);
            Log4j2Utils.addAppenderForAllLoggers(kafkaAppender);
        }

        private KafkaAppender buildKafkaAppender() {
            LoggerContext loggerContext = Log4j2Utils.getLoggerContext();
            Log4j2KafkaAppenderProperties properties = this.kafkaAppenderProperties;

            KafkaAppender.Builder builder = newBuilder()
                    .setName(properties.getName())
                    .setIgnoreExceptions(properties.isIgnoreExceptions())
                    .setKey(properties.getKey())
                    .setSyncSend(properties.isSyncSend())
                    // .setSendEventTimestamp(properties.isSendEventTimestamp())
                    .setTopic(properties.getTopic())
                    .setLayout(getLayout(loggerContext))
                    .setFilter(getFilter())
                    .setPropertyArray(getProperties())
                    .setConfiguration(loggerContext.getConfiguration());

            KafkaAppender kafkaAppender = builder.build();
            // start-up Kafka Client
            kafkaAppender.start();
            return kafkaAppender;
        }

        private Layout<? extends Serializable> getLayout(LoggerContext loggerContext) {
            String layoutBeanName = this.kafkaAppenderProperties.getLayout();
            // First, try to use Spring Bean
            if (hasText(layoutBeanName)) {
                return getBeanIfAvailable(this.context, layoutBeanName, Layout.class);
            }

            // Second, try to se the pattern
            String patternLayout = this.kafkaAppenderProperties.getPatternLayout();
            if (hasText(patternLayout)) {
                return PatternLayout.newBuilder().withPattern(patternLayout).build();
            }

            // Finally, use DefaultKafkaLayout
            return new DefaultKafkaLayout(loggerContext);
        }

        private Filter getFilter() {
            String filterBeanName = this.kafkaAppenderProperties.getFilter();
            if (hasText(filterBeanName)) {
                return getBeanIfAvailable(this.context, filterBeanName, Filter.class);
            }
            return null;
        }

        private Property[] getProperties() {
            Map<String, String> kafkaProperties = this.kafkaAppenderProperties.getProperties();
            Property[] properties = new Property[kafkaProperties.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : kafkaProperties.entrySet()) {
                properties[i++] = createProperty(entry.getKey(), entry.getValue());
            }
            return properties;
        }
    }


}
