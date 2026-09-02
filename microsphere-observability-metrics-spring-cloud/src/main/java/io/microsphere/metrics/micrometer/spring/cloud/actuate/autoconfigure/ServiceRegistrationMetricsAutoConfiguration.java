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
package io.microsphere.metrics.micrometer.spring.cloud.actuate.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerAvailable;
import io.microsphere.spring.cloud.client.service.registry.condition.ConditionalOnAutoServiceRegistrationAvailable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

;

/**
 * Micrometer Auto-Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see MeterRegistry
 * @see org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
 * @see org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
 * @since 1.0.0
 */
@ConditionalOnDiscoveryEnabled
@ConditionalOnMicrometerAvailable
@ConditionalOnAutoServiceRegistrationAvailable
@AutoConfigureAfter(name = {
        // Spring Boot Actuator API [2.0, 4.0)
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
        // Spring Boot Actuator API [4.0, )
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",

        "io.microsphere.spring.cloud.client.service.registry.autoconfigure.SimpleAutoServiceRegistrationAutoConfiguration"
})
@Configuration(proxyBeanMethods = false)
public class ServiceRegistrationMetricsAutoConfiguration {

    public static final String INSTANCE_TAG_KEY = "instance";

    @Bean
    public MeterRegistryCustomizer commonMeterRegistryCustomizer(
            ObjectProvider<Registration> registrationProvider) {
        return registry -> {
            configureCommonTags(registry, registrationProvider);
        };
    }

    private void configureCommonTags(MeterRegistry registry, ObjectProvider<Registration> registrationProvider) {
        registrationProvider.ifAvailable(registration -> {
            String host = registration.getHost();
            String instance = host + ":" + registration.getPort();
            registry.config().commonTags(INSTANCE_TAG_KEY, instance);
        });
    }
}
