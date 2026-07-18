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

package io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure;

import io.microsphere.annotation.ConfigurationProperty;
import io.microsphere.constants.PropertyConstants;
import io.microsphere.metrics.micrometer.instrument.binder.system.CGroupMemoryMetrics;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnCGroup;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;
import static io.microsphere.constants.SymbolConstants.DOT;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.CGGroupMetricsAutoConfiguration.ENABLED_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled.PREFIX;

/**
 * The Auto-Configuration class for CGroup Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SystemMetricsAutoConfiguration
 * @see CGroupMemoryMetrics
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnCGroup
@ConditionalOnClass(name = {
        "io.microsphere.metrics.micrometer.instrument.binder.system.CGroupMemoryMetrics"  // Microsphere Observability Micrometer API
})
@ConditionalOnProperty(name = ENABLED_PROPERTY_NAME, matchIfMissing = true)
@ConditionalOnMicrometerEnabled
public class CGGroupMetricsAutoConfiguration {

    /**
     * The Property Name of enabling CGroup metrics : "microsphere.metrics.micrometer.cgroup.enabled"
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = "true",
            source = APPLICATION_SOURCE
    )
    public static final String ENABLED_PROPERTY_NAME = PREFIX + "cgroup" + DOT + PropertyConstants.ENABLED_PROPERTY_NAME;

    @Bean
    @ConditionalOnMissingBean
    public CGroupMemoryMetrics cGroupMemoryMetrics() {
        return new CGroupMemoryMetrics();
    }
}