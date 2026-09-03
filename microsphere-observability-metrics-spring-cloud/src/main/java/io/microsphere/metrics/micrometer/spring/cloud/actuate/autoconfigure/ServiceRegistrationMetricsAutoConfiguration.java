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
import io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector;
import io.microsphere.spring.beans.factory.config.GenericBeanPostProcessorAdapter;
import io.microsphere.spring.cloud.client.service.registry.condition.ConditionalOnAutoServiceRegistrationAvailable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

;

/**
 * Micrometer Auto-Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see MeterRegistry
 * @see org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
 * @see org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
 * @see org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration
 * @see io.microsphere.spring.cloud.client.service.registry.autoconfigure.SimpleAutoServiceRegistrationAutoConfiguration
 * @see io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SentinelMetricsAutoConfiguration
 * @since 1.0.0
 */
@ConditionalOnDiscoveryEnabled
@ConditionalOnMicrometerAvailable
@ConditionalOnAutoServiceRegistrationAvailable
@AutoConfigureAfter(name = {
        // Spring Boot Actuator API [2.0, 4.0)
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration",

        // Spring Boot Actuator API [4.0, )
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration",

        "io.microsphere.spring.cloud.client.service.registry.autoconfigure.SimpleAutoServiceRegistrationAutoConfiguration",
        "io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SentinelMetricsAutoConfiguration"
})
@Import(ServiceRegistrationMetricsAutoConfiguration.PrometheusConfig.class)
@Configuration(proxyBeanMethods = false)
public class ServiceRegistrationMetricsAutoConfiguration {

    public static final String INSTANCE_TAG_KEY = "instance";

    private static String instance;

    public ServiceRegistrationMetricsAutoConfiguration(ObjectProvider<Registration> registrationProvider, @Value("${server.port:-1}") int port) {
        registrationProvider.ifAvailable(registration -> {
            String host = registration.getHost();
            instance = host + ":" + port;
        });
    }

    @Bean
    public MeterRegistryCustomizer commonMeterRegistryCustomizer() {
        return registry -> registry.config().commonTags(INSTANCE_TAG_KEY, instance);
    }

    @ConditionalOnClass(name = {
            "io.micrometer.prometheusmetrics.PrometheusMeterRegistry"
    })
    static class PrometheusConfig {

        @ConditionalOnClass(name = {
                "io.prometheus.metrics.model.registry.MultiCollector"
        })
        @ConditionalOnBean(type = {
                "io.micrometer.prometheusmetrics.PrometheusMeterRegistry",
                "io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector"
        })
        @Bean
        public BeanPostProcessor sentinelMultiCollectorBeanPostProcessor() {
            return new GenericBeanPostProcessorAdapter<SentinelMultiCollector>() {
                @Override
                protected void processBeforeInitialization(SentinelMultiCollector bean, String beanName) {
                    bean.commonLabel(INSTANCE_TAG_KEY, instance);
                }
            };
        }
    }
}
