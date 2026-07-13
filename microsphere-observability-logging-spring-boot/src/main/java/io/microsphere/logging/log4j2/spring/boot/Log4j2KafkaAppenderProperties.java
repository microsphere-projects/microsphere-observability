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
package io.microsphere.logging.log4j2.spring.boot;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.mom.kafka.KafkaAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

import static io.microsphere.logging.log4j2.spring.boot.Log4j2KafkaAppenderProperties.PREFIX;
import static java.util.Collections.emptyMap;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;

/**
 * The {@link ConfigurationProperties @ConfigurationProperties} for Log4j2's {@link KafkaAppender}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see ConfigurationProperties
 * @see KafkaAppender
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = PREFIX)
public class Log4j2KafkaAppenderProperties {

    /**
     * The property prefix of {@link ConfigurationProperties @ConfigurationProperties} for Log4j2's {@link KafkaAppender}
     * : "microsphere.log4j2.kafka.appender"
     */
    public static final String PREFIX = "microsphere.log4j2.kafka.appender";

    /**
     * The Kafka topic to use, default value is "java-app-logs"
     */
    private String topic = "java-app-logs";

    /**
     * The key that will be sent to Kafka with every message. Optional value defaulting to null.
     */
    private String key;

    /**
     * The bean name of {@link Filter}
     * A Filter to determine if the event should be handled by this Appender. More than one Filter may be used by using a CompositeFilter.
     */
    private String filter;

    /**
     * The bean name of {@link Layout}
     * The Layout to use to format the LogEvent. Optional, <PatternLayout pattern="%m"/> was default.
     */
    private String layout;

    /**
     * The pattern value of {@link PatternLayout}, the default value is "%ms"
     */
    private String patternLayout = "%m";

    /**
     * The name of the Appender. Required.
     */
    private String name = "microsphere-kafka-appender";

    /**
     * The default is true, causing exceptions encountered while appending events to be internally logged and then ignored.
     * When set to false exceptions will be propagated to the caller, instead. You must set this to false when wrapping
     * this Appender in a FailoverAppender.
     */
    private boolean ignoreExceptions = true;

    /**
     * The retry count when the message sending is failed.
     */
    private int retryCount = 3;

    /**
     * The default is true, causing sends to block until the record has been acknowledged by the Kafka server.
     * When set to false sends return immediately, allowing for lower latency and significantly higher throughput.
     */
    private boolean syncSend = true;

    /**
     * The default is false, whether sends the log event timestamp as the Kafka Records' timestamp or not.
     */
    private boolean sendEventTimestamp = false;

    /**
     * You can set properties in Kafka producer properties. You need to set the "bootstrap.servers" property,
     * there are sensible default values for the others. Do not set the "value.serializer" nor "key.serializer" properties.
     */
    private Map<String, String> properties;

    public boolean isEnabled() {
        Map<String, String> properties = getProperties();
        return properties.containsKey(BOOTSTRAP_SERVERS_CONFIG);
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getPatternLayout() {
        return patternLayout;
    }

    public void setPatternLayout(String patternLayout) {
        this.patternLayout = patternLayout;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIgnoreExceptions() {
        return ignoreExceptions;
    }

    public void setIgnoreExceptions(boolean ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public boolean isSyncSend() {
        return syncSend;
    }

    public void setSyncSend(boolean syncSend) {
        this.syncSend = syncSend;
    }

    public boolean isSendEventTimestamp() {
        return sendEventTimestamp;
    }

    public void setSendEventTimestamp(boolean sendEventTimestamp) {
        this.sendEventTimestamp = sendEventTimestamp;
    }

    public Map<String, String> getProperties() {
        if (properties == null) {
            return emptyMap();
        }
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
