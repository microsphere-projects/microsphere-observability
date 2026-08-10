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

package io.microsphere.observability.logging.util;

import io.microsphere.logging.Logger;
import io.microsphere.logging.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.microsphere.util.ClassUtils.getTypeName;
import static io.microsphere.util.StringUtils.substringBefore;

/**
 * The Utilities class of Logger
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Logger
 * @see LoggerFactory
 * @since 1.0.0
 */
public abstract class LoggerUtils {

    public static void trace(Consumer<Logger> loggerConsumer) {
        log(Logger::isTraceEnabled, loggerConsumer);
    }

    public static void debug(Consumer<Logger> loggerConsumer) {
        log(Logger::isDebugEnabled, loggerConsumer);
    }

    public static void info(Consumer<Logger> loggerConsumer) {
        log(Logger::isInfoEnabled, loggerConsumer);
    }

    public static void warn(Consumer<Logger> loggerConsumer) {
        log(Logger::isWarnEnabled, loggerConsumer);
    }

    public static void error(Consumer<Logger> loggerConsumer) {
        log(Logger::isErrorEnabled, loggerConsumer);
    }

    static void log(Predicate<Logger> loggerPredicate, Consumer<Logger> loggerConsumer) {
        Logger logger = getLogger(loggerConsumer);
        if (loggerPredicate.test(logger)) {
            loggerConsumer.accept(logger);
        }
    }

    private static Logger getLogger(Consumer<Logger> loggerConsumer) {
        String loggerName = getLoggerName(loggerConsumer);
        return LoggerFactory.getLogger(loggerName);
    }

    private static String getLoggerName(Consumer<Logger> loggerConsumer) {
        return substringBefore(getTypeName(loggerConsumer), "$");
    }

    private LoggerUtils() {
    }
}