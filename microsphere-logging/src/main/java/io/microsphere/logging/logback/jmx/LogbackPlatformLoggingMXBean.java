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
package io.microsphere.logging.logback.jmx;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.microsphere.logging.management.AbstractPlatformLoggingMXBean;

import java.lang.management.PlatformLoggingMXBean;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link PlatformLoggingMXBean} for logback
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see PlatformLoggingMXBean
 * @since 1.0.0
 */
public class LogbackPlatformLoggingMXBean extends AbstractPlatformLoggingMXBean<Logger> {

    private final LoggerContext loggerContext;

    public LogbackPlatformLoggingMXBean(Logger logger) {
        this(logger, Collections.emptyMap());
    }

    public LogbackPlatformLoggingMXBean(Logger logger, Map<String, String> properties) {
        super(logger, properties);
        this.loggerContext = logger.getLoggerContext();
    }

    @Override
    public List<String> getLoggerNames() {
        return this.loggerContext.getLoggerList()
                .stream()
                .map(Logger::getName)
                .toList();
    }

    @Override
    public String getLoggerLevel(String loggerName) {
        Logger logger = this.loggerContext.getLogger(loggerName);
        Level level = logger.getLevel();
        return level == null ? null : level.toString();
    }

    @Override
    public void setLoggerLevel(String loggerName, String levelName) {
        Level level = Level.toLevel(levelName);
        if (!"debug".equalsIgnoreCase(levelName) && Level.DEBUG.equals(level)) {
            return;
        }
        Logger logger = this.loggerContext.getLogger(loggerName);
        logger.setLevel(level);
    }

    @Override
    public String getParentLoggerName(String loggerName) {
        Logger logger = this.loggerContext.getLogger(loggerName);
        // TODO
        return "";
    }
}
