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
package io.microsphere.logging.log4j2;

import io.microsphere.logging.log4j2.appender.InMemoryAppender;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.apache.logging.log4j.core.layout.PatternLayout.newBuilder;

/**
 * Default Kafka {@link Layout}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Layout
 * @since 1.0.0
 */
public class DefaultKafkaLayout<T extends Serializable> implements Layout<T> {

    public static final Layout DEFAULT_LAYOUT = newBuilder().withPattern("%m").build();

    private final Map<String, Layout> delegatingLayouts;

    public DefaultKafkaLayout(LoggerContext context) {
        this.delegatingLayouts = initDelegatingLayouts(context);
    }

    private Map<String, Layout> initDelegatingLayouts(LoggerContext context) {
        Map<String, Layout> delegatingLayouts = new HashMap<>();
        for (Logger logger : context.getLoggers()) {
            String loggerName = logger.getName();
            Layout layout = selectLayout(logger);
            delegatingLayouts.put(loggerName, layout);
        }
        return delegatingLayouts;
    }

    private Layout selectLayout(Logger logger) {
        Appender appender = selectAppender(logger);
        Layout layout = appender == null ? null : appender.getLayout();
        return layout == null ? DEFAULT_LAYOUT : layout;
    }

    private Appender selectAppender(Logger logger) {
        Appender targetAppender = selectAppender(logger, RollingFileAppender.class);
        if (targetAppender == null) {
            targetAppender = selectAppender(logger, FileAppender.class);
        }
        return targetAppender;
    }

    private Appender selectAppender(Logger logger, Class<? extends Appender> appenderClass) {
        Appender targetAppender = null;
        Map<String, Appender> appendersMap = new HashMap<>(logger.getAppenders());
        appendersMap.remove(InMemoryAppender.NAME);
        if (appendersMap.isEmpty()) {
            Logger parentLogger = logger.getParent();
            if (parentLogger != null) {
                targetAppender = selectAppender(parentLogger, appenderClass);
            }
        } else {
            for (Map.Entry<String, Appender> entry : appendersMap.entrySet()) {
                Appender appender = entry.getValue();
                if (appenderClass.isAssignableFrom(appender.getClass())) {
                    targetAppender = appender;
                    break;
                }
            }
        }

        return targetAppender;
    }


    @Override
    public byte[] getFooter() {
        return DEFAULT_LAYOUT.getFooter();
    }

    @Override
    public byte[] getHeader() {
        return DEFAULT_LAYOUT.getHeader();
    }

    @Override
    public byte[] toByteArray(LogEvent event) {
        Layout delegate = getDelegate(event);
        return delegate.toByteArray(event);
    }

    @Override
    public T toSerializable(LogEvent event) {
        Layout delegate = getDelegate(event);
        return (T) delegate.toSerializable(event);
    }

    @Override
    public String getContentType() {
        return DEFAULT_LAYOUT.getContentType();
    }

    @Override
    public Map<String, String> getContentFormat() {
        return DEFAULT_LAYOUT.getContentFormat();
    }

    @Override
    public void encode(LogEvent source, ByteBufferDestination destination) {
        Layout delegate = getDelegate(source);
        delegate.encode(source, destination);
    }

    private Layout getDelegate(LogEvent event) {
        String loggerName = event.getLoggerName();
        return delegatingLayouts.getOrDefault(loggerName, DEFAULT_LAYOUT);
    }
}
