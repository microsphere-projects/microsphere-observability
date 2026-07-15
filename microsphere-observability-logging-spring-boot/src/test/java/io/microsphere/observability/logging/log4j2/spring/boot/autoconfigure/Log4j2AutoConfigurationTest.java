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

package io.microsphere.observability.logging.log4j2.spring.boot.autoconfigure;


import io.microsphere.observability.logging.log4j2.spring.boot.autoconfigure.Log4j2AutoConfiguration.KafkaAppenderConfiguration;
import io.microsphere.spring.boot.test.AutoConfigurationTest;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link Log4j2AutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Log4j2AutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                Log4j2AutoConfigurationTest.class
        },
        webEnvironment = NONE,
        properties = {
                "microsphere.log4j2.kafka.appender.properties.bootstrap.servers=localhost:9092"
        }
)
class Log4j2AutoConfigurationTest extends AutoConfigurationTest<Log4j2AutoConfiguration> {

    @Override
    protected void configureAutoConfiguredClasses(Set<Class<?>> autoConfiguredClasses) {
        autoConfiguredClasses.add(KafkaAppenderConfiguration.class);
    }

    @Override
    protected void configureGlobalDisabledPropertyValues(Set<String> globalDisabledPropertyValues) {
    }

    @Override
    protected void configureGlobalMissingClasses(Set<Class<?>> globalMissingClasses) {
        globalMissingClasses.add(LoggerContextFactory.class);
        globalMissingClasses.add(Log4jContextFactory.class);
    }
}