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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static io.microsphere.metrics.micrometer.spring.cloud.actuate.autoconfigure.ServiceRegistrationMetricsAutoConfiguration.INSTANCE_TAG_KEY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * {@link ServiceRegistrationMetricsAutoConfiguration} Integration Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ServiceRegistrationMetricsAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                ServiceRegistrationMetricsAutoConfigurationIntegrationTest.class
        },
        webEnvironment = RANDOM_PORT,
        properties = {
                "management.metrics.export.prometheus.enabled=true",
                "microsphere.spring.cloud.service-registry.auto-registration.simple.enabled=true"
        }
)
@EnableAutoConfiguration
public class ServiceRegistrationMetricsAutoConfigurationIntegrationTest {

    @Autowired
    private MeterRegistry registry;

    @Test
    void test() {
        List<Meter> meters = registry.getMeters();
        meters.stream().map(Meter::getId).forEach(meterId -> {
            assertNotNull(meterId.getTag("application"));
            assertNotNull(meterId.getTag(INSTANCE_TAG_KEY));
            assertNotNull(meterId.getTag("profiles"));
        });
    }
}
