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
package io.microsphere.metrics.micrometer.spring.boot.actuate.condition;

import io.micrometer.core.instrument.MeterRegistry;
import io.microsphere.annotation.ConfigurationProperty;
import io.microsphere.constants.PropertyConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Spring Boot Condition for Micrometer Enabled
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see MeterRegistry
 * @since 1.0.0
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Documented
@ConditionalOnProperty(name = ConditionalOnMicrometerEnabled.ENABLED_PROPERTY_NAME, matchIfMissing = true)
@ConditionalOnClass(name = {"io.micrometer.core.instrument.MeterRegistry"})
public @interface ConditionalOnMicrometerEnabled {

    /**
     * The property name prefix for Micrometer : "microsphere.metrics.micrometer."
     */
    String PREFIX = "microsphere.metrics.micrometer.";

    /**
     * The property name for enabling Micrometer features : "microsphere.metrics.micrometer.enabled"
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = "true",
            source = APPLICATION_SOURCE
    )
    String ENABLED_PROPERTY_NAME = PREFIX + PropertyConstants.ENABLED_PROPERTY_NAME;
}
