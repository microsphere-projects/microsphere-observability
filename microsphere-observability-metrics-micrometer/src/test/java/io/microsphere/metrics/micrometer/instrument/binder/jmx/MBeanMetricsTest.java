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

package io.microsphere.metrics.micrometer.instrument.binder.jmx;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link MBeanMetrics} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see MBeanMetrics
 * @since 1.0.0
 */
class MBeanMetricsTest {

    private static final String metricName = "logger.names.count";

    private MeterRegistry registry;

    private MBeanMetrics mBeanMetrics;

    @BeforeEach
    void setUp() throws MalformedObjectNameException {
        ObjectName objectNameToQuery = new ObjectName("java.util.logging:type=Logging");
        this.registry = new SimpleMeterRegistry();
        this.mBeanMetrics = new MBeanMetrics(objectNameToQuery, (mBeanName, mBeanInfo, mBeanAttributeInfo, attributeValue, registry) -> {
            if ("LoggerNames".equals(mBeanAttributeInfo.getName())) {
                String[] loggerNames = (String[]) attributeValue;
                registry.counter(metricName)
                        .increment(loggerNames.length);
            }
        });
    }

    @Test
    void test() {
        this.mBeanMetrics.bindTo(this.registry);

        long count = this.registry.getMeters()
                .stream()
                .filter(meter -> meter.getId().getName().equals(metricName))
                .count();

        assertEquals(1, count);
    }
}