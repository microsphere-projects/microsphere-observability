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
package io.microsphere.logging.log4j2.filter.i18n;

import io.microsphere.i18n.ServiceMessageSource;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;

import static io.microsphere.util.StringUtils.substringBetween;

/**
 * Log4j2 Filter based on based on <a href="https://github.com/microsphere-projects/microsphere-i18n">microsphere-i18n</a>
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class I18nLog4j2Filter extends AbstractFilter {

    public static final String DEFAULT_MESSAGE_PATTERN_PREFIX = "{";

    public static final String DEFAULT_MESSAGE_PATTERN_SUFFIX = "}";

    private final ServiceMessageSource serviceMessageSource;

    private final String messagePatternPrefix;

    private final String messagePatternSuffix;

    public I18nLog4j2Filter(ServiceMessageSource serviceMessageSource) {
        this(serviceMessageSource, DEFAULT_MESSAGE_PATTERN_PREFIX, DEFAULT_MESSAGE_PATTERN_SUFFIX);
    }

    public I18nLog4j2Filter(ServiceMessageSource serviceMessageSource, String messagePatternPrefix, String messagePatternSuffix) {
        this.serviceMessageSource = serviceMessageSource;
        this.messagePatternPrefix = messagePatternPrefix;
        this.messagePatternSuffix = messagePatternSuffix;
    }


    @Override
    public Result filter(LogEvent event) {
        MutableLogEvent mutableLogEvent = new MutableLogEvent();
        // Copy the original event
        mutableLogEvent.initFrom(event);
        // interpolate the formatted message
        interpolate(mutableLogEvent);
        // publish the MutableLogEvent
        return super.filter(mutableLogEvent);
    }

    private void interpolate(MutableLogEvent event) {
        String formattedMessage = event.getFormattedMessage();
        Object[] parameters = event.getParameters();
        String resolvedCode = resolveCode(formattedMessage);
        String localizedMessage = serviceMessageSource.getMessage(resolvedCode, parameters);
        Message newMessage = rebuildMessage(event, localizedMessage);
        event.setMessage(newMessage);
    }

    private Message rebuildMessage(MutableLogEvent event, String localizedMessage) {
        Message message = event.getMessage();
        if (message instanceof SimpleMessage) {
            return new SimpleMessage(localizedMessage);
        }
        // TODO support more Message types
        return message;
    }

    private String resolveCode(String formattedMessage) {
        return substringBetween(formattedMessage, messagePatternPrefix, messagePatternSuffix);
    }
}
