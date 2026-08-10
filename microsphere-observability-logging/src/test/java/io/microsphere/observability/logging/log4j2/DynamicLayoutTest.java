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

package io.microsphere.observability.logging.log4j2;


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.microsphere.logging.log4j2.util.Log4j2Utils.getLoggerContext;
import static io.microsphere.observability.logging.log4j2.DynamicLayout.DEFAULT_LAYOUT;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.apache.logging.log4j.core.impl.Log4jLogEvent.newBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link DynamicLayout} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see DynamicLayout
 * @since 1.0.0
 */
class DynamicLayoutTest {

    private static final Logger[] LOGGERS = {
            getLogger(DynamicLayoutTest.class),
            getLogger("io.microsphere.observability.logging.log4j2"),
            getLogger("io.microsphere.observability.logging"),
            getLogger("io.microsphere.observability"),
            getLogger("io.microsphere"),
            getLogger("io"),
            getLogger("")
    };

    private LoggerContext loggerContext;

    private DynamicLayout dynamicLayout;

    @BeforeEach
    void setUp() {
        this.loggerContext = getLoggerContext();
        this.dynamicLayout = new DynamicLayout(this.loggerContext);
    }

    @Test
    void test() {
        assertEquals(DEFAULT_LAYOUT.getFooter(), this.dynamicLayout.getFooter());
        assertEquals(DEFAULT_LAYOUT.getHeader(), this.dynamicLayout.getHeader());
        assertEquals(DEFAULT_LAYOUT.getContentType(), this.dynamicLayout.getContentType());
        assertEquals(DEFAULT_LAYOUT.getContentFormat(), this.dynamicLayout.getContentFormat());

        for (Logger logger : LOGGERS) {
            LogEvent logEvent = newBuilder()
                    .setLevel(logger.getLevel())
                    .setLoggerName(logger.getName())
                    .setMessage(new SimpleMessage("test"))
                    .build();

            assertNotNull(this.dynamicLayout.toByteArray(logEvent));
            assertNotNull(this.dynamicLayout.toSerializable(logEvent));

            Map<String, Appender> appenders = ((org.apache.logging.log4j.core.Logger) logger).getAppenders();
            for (Appender appender : appenders.values()) {
                if (appender instanceof AbstractOutputStreamAppender) {
                    AbstractOutputStreamAppender aa = (AbstractOutputStreamAppender) appender;
                    this.dynamicLayout.encode(logEvent, aa.getManager());
                }
            }
        }
    }
}