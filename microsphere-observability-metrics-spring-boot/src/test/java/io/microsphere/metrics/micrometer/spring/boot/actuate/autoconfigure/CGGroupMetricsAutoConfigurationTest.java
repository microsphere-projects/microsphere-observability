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


import io.micrometer.core.instrument.MeterRegistry;
import io.microsphere.metrics.micrometer.instrument.binder.system.CGroupMemoryMetrics;
import io.microsphere.spring.boot.test.AutoConfigurationTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link CGGroupMetricsAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see CGGroupMetricsAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                CGGroupMetricsAutoConfigurationTest.class
        },
        webEnvironment = NONE,
        properties = {
                "cgroup.dir=file://${user.dir}"
        }
)
class CGGroupMetricsAutoConfigurationTest extends AutoConfigurationTest<CGGroupMetricsAutoConfiguration> {

    @Override
    protected void configureAutoConfiguredClasses(Set<Class<?>> autoConfiguredClasses) {
        autoConfiguredClasses.add(CGroupMemoryMetrics.class);
    }

    @Override
    protected void configureGlobalDisabledPropertyValues(Set<String> globalDisabledPropertyValues) {
        globalDisabledPropertyValues.add("microsphere.metrics.micrometer.cgroup.enabled=false");
        globalDisabledPropertyValues.add("microsphere.metrics.micrometer.enabled=false");
        globalDisabledPropertyValues.add("cgroup.dir=file://not-found");
    }

    @Override
    protected void configureGlobalMissingClasses(Set<Class<?>> globalMissingClasses) {
        globalMissingClasses.add(CGroupMemoryMetrics.class);
        globalMissingClasses.add(MeterRegistry.class);
    }
}