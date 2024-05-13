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
package io.microsphere.logging.slf4j;

import io.microsphere.i18n.ServiceMessageSource;
import org.slf4j.Logger;
import org.slf4j.Marker;

import static io.microsphere.util.ArrayUtils.of;
import static io.microsphere.util.StringUtils.substringBetween;

/**
 * {@link Logger} implementation based on <a href="https://github.com/microsphere-projects/microsphere-i18n">microsphere-i18n</a>
 *
 * TODO : Finish
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class I18nLogger implements Logger {

    public static final String DEFAULT_MESSAGE_PATTERN_PREFIX = "{";
    public static final String DEFAULT_MESSAGE_PATTERN_SUFFIX = "}";

    private final ServiceMessageSource serviceMessageSource;

    private final Logger delegate;

    private final String messagePatternPrefix;

    private final String messagePatternSuffix;

    public I18nLogger(ServiceMessageSource serviceMessageSource, Logger delegate) {
        this(serviceMessageSource, delegate, DEFAULT_MESSAGE_PATTERN_PREFIX, DEFAULT_MESSAGE_PATTERN_SUFFIX);
    }

    public I18nLogger(ServiceMessageSource serviceMessageSource, Logger delegate,
                      String messagePatternPrefix, String messagePatternSuffix) {
        this.serviceMessageSource = serviceMessageSource;
        this.delegate = delegate;
        this.messagePatternPrefix = messagePatternPrefix;
        this.messagePatternSuffix = messagePatternSuffix;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        Object[] args = of();
        trace(msg, args);
    }

    @Override
    public void trace(String format, Object arg) {
        Object[] args = of(arg);
        trace(format, args);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        Object[] args = of(arg1, arg2);
        trace(format, args);
    }

    @Override
    public void trace(String messagePattern, Object... arguments) {
        // logger.trace("{common.not.null}","abc",e);
        String code = resolveCode(messagePattern);
        String message = serviceMessageSource.getMessage(code, arguments);
        Object lastArgument = arguments.length == 0 ? null : arguments[arguments.length - 1];
        if (lastArgument instanceof Throwable) {
            delegate.trace(message, (Throwable) lastArgument);
        } else {
            delegate.trace(message);
        }
    }

    private String resolveCode(String messagePattern) {
        return substringBetween(messagePattern, messagePatternPrefix, messagePatternSuffix);
    }

    @Override
    public void trace(String msg, Throwable t) {
        Object[] args = of(t);
        trace(msg, args);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String msg) {

    }

    @Override
    public void trace(Marker marker, String format, Object arg) {

    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {

    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {

    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {

    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(String msg) {

    }

    @Override
    public void debug(String format, Object arg) {

    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {

    }

    @Override
    public void debug(String format, Object... arguments) {

    }

    @Override
    public void debug(String msg, Throwable t) {

    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    @Override
    public void debug(Marker marker, String msg) {

    }

    @Override
    public void debug(Marker marker, String format, Object arg) {

    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {

    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {

    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {

    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public void info(String msg) {

    }

    @Override
    public void info(String format, Object arg) {

    }

    @Override
    public void info(String format, Object arg1, Object arg2) {

    }

    @Override
    public void info(String format, Object... arguments) {

    }

    @Override
    public void info(String msg, Throwable t) {

    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    @Override
    public void info(Marker marker, String msg) {

    }

    @Override
    public void info(Marker marker, String format, Object arg) {

    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {

    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {

    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {

    }

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public void warn(String msg) {

    }

    @Override
    public void warn(String format, Object arg) {

    }

    @Override
    public void warn(String format, Object... arguments) {

    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {

    }

    @Override
    public void warn(String msg, Throwable t) {

    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return false;
    }

    @Override
    public void warn(Marker marker, String msg) {

    }

    @Override
    public void warn(Marker marker, String format, Object arg) {

    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {

    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {

    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {

    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public void error(String msg) {

    }

    @Override
    public void error(String format, Object arg) {

    }

    @Override
    public void error(String format, Object arg1, Object arg2) {

    }

    @Override
    public void error(String format, Object... arguments) {

    }

    @Override
    public void error(String msg, Throwable t) {

    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return false;
    }

    @Override
    public void error(Marker marker, String msg) {

    }

    @Override
    public void error(Marker marker, String format, Object arg) {

    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {

    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {

    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {

    }
}
