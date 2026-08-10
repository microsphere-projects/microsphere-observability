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

package io.microsphere.observability.logging.spring.boot.autoconfigure;


import io.microsphere.observability.logging.spring.boot.autoconfigure.ApplicationLoggingAutoConfiguration.LoggingUncaughtExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.lang.Thread.UncaughtExceptionHandler;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.getDefaultUncaughtExceptionHandler;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link ApplicationLoggingAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ApplicationLoggingAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                ApplicationLoggingAutoConfiguration.class,
                ApplicationLoggingAutoConfigurationTest.class
        },
        webEnvironment = NONE
)
class ApplicationLoggingAutoConfigurationTest {

    @Autowired
    private ApplicationLoggingAutoConfiguration applicationLoggingAutoConfiguration;

    @Test
    void test() {
        assertNotNull(applicationLoggingAutoConfiguration);

        LoggingUncaughtExceptionHandler uncaughtExceptionHandler = new LoggingUncaughtExceptionHandler(null);
        assertUncaughtExceptionHandler(uncaughtExceptionHandler);
        assertUncaughtExceptionHandler(getDefaultUncaughtExceptionHandler());

        testApplicationFailedEvent();
    }

    void testApplicationFailedEvent() {
        assertThrows(IllegalStateException.class, () -> {
            new SpringApplicationBuilder(ApplicationLoggingAutoConfiguration.class, ErrorConfig.class)
                    .web(WebApplicationType.NONE)
                    .run();
        });
    }

    void assertUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        uncaughtExceptionHandler.uncaughtException(currentThread(), new Throwable("For testing"));
    }

    static class ErrorConfig {

        @Bean
        public ApplicationRunner runner() {
            return args -> {
                throw new IllegalStateException("For testing");
            };
        }
    }
}