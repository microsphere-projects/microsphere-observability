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
package io.microsphere.logging.log4j2.util;

import io.microsphere.util.BaseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * The utilities class for Log4j2
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see LoggerContext
 * @see Configuration
 * @see Logger
 * @since 1.0.0
 */
public abstract class Log4j2Utils extends BaseUtils {

    /**
     * Get the {@link Configuration}
     *
     * @return non-null
     */
    public static Configuration getConfiguration() {
        LoggerContext loggerContext = getLoggerContext();
        return loggerContext.getConfiguration();
    }

    /**
     * Find the {@link Appender} by its name
     *
     * @param appenderName the name of {@link Appender}
     * @param <T>          the type of {@link Appender}
     * @return <code>null</code> if can't be found
     */
    public static <T extends Appender> T findAppender(String appenderName) {
        Configuration configuration = getConfiguration();
        return configuration.getAppender(appenderName);
    }

    /**
     * Get the {@link LoggerContext}
     *
     * @return non-null
     */
    public static LoggerContext getLoggerContext() {
        return (LoggerContext) LogManager.getContext(false);
    }

    /**
     * Get all {@link Logger loggers}
     *
     * @return non-null
     */
    public static Collection<Logger> getLoggers() {
        return getLoggerContext().getLoggers();
    }


    public static void doInLogger(Consumer<Logger> loggerConsumer) {
        getLoggers().forEach(loggerConsumer);
    }

    public static void addAppender(Appender appender, Iterable<Logger> loggers) {
        LoggerContext loggerContext = getLoggerContext();
        addAppender(loggerContext, appender, loggers);
    }

    public static void addAppenderForAllLoggers(Appender appender) {
        LoggerContext loggerContext = getLoggerContext();
        addAppender(loggerContext, appender, loggerContext.getLoggers());
    }

    public static void removeAppender(Appender appender, Iterable<Logger> loggers) {
        LoggerContext loggerContext = getLoggerContext();
        removeAppender(loggerContext, appender, loggers);
    }

    public static void removeAppenderForAllLoggers(Appender appender) {
        LoggerContext loggerContext = getLoggerContext();
        removeAppender(loggerContext, appender, loggerContext.getLoggers());
    }

    static void addAppender(LoggerContext loggerContext, Appender appender, Iterable<Logger> loggers) {
        if (loggerContext == null || appender == null || loggers == null) {
            return;
        }
        Configuration configuration = loggerContext.getConfiguration();
        synchronized (loggerContext) {
            if (!appender.isStarted()) {
                appender.start();
            }
            configuration.addAppender(appender);
            for (Logger logger : loggers) {
                LoggerConfig loggerConfig = logger.get();
                loggerConfig.addAppender(appender, null, null);
            }
            loggerContext.updateLoggers();
        }
    }

    static void removeAppender(LoggerContext loggerContext, Appender appender, Iterable<Logger> loggers) {
        if (loggerContext == null || appender == null || loggers == null) {
            return;
        }
        Configuration configuration = loggerContext.getConfiguration();
        String appenderName = appender.getName();
        synchronized (loggerContext) {
            if (!appender.isStopped()) {
                appender.stop();
            }
            configuration.getAppenders().remove(appenderName);
            for (Logger logger : loggers) {
                LoggerConfig loggerConfig = logger.get();
                loggerConfig.removeAppender(appenderName);
            }
            loggerContext.updateLoggers();
        }
    }
}
