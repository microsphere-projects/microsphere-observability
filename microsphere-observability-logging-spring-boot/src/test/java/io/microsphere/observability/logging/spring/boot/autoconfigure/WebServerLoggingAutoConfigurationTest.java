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


import io.microsphere.logging.test.jupiter.LoggingLevelsClass;
import io.microsphere.spring.boot.test.WebAutoConfigurationTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

/**
 * {@link WebServerLoggingAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebServerLoggingAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                WebServerLoggingAutoConfigurationTest.class
        }
)
@LoggingLevelsClass(
        loggingClasses = {
                WebServerLoggingAutoConfiguration.class
        },
        levels = {
                "TRACE",
                "INFO",
                "ERROR"
        }
)
class WebServerLoggingAutoConfigurationTest extends WebAutoConfigurationTest<WebServerLoggingAutoConfiguration> {

    @Override
    protected void configureAutoConfiguredClasses(Set<Class<?>> autoConfiguredClasses) {
    }

    @Override
    protected void configureGlobalDisabledPropertyValues(Set<String> globalDisabledPropertyValues) {
    }

    @Override
    protected void configureGlobalMissingClasses(Set<Class<?>> globalMissingClasses) {
    }
}