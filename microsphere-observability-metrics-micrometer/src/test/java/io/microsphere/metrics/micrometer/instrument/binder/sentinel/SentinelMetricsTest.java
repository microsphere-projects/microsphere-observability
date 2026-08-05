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

package io.microsphere.metrics.micrometer.instrument.binder.sentinel;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.microsphere.alibaba.sentinel.common.SentinelTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.alibaba.csp.sentinel.slots.statistic.StatisticSlotCallbackRegistry.clearEntryCallback;
import static com.alibaba.csp.sentinel.slots.statistic.StatisticSlotCallbackRegistry.clearExitCallback;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SentinelMetrics} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMetrics
 * @since 1.0.0
 */
class SentinelMetricsTest {

    private MeterRegistry registry;

    private SentinelMetrics sentinelMetrics;

    @BeforeEach
    void setUp() {
        this.registry = new SimpleMeterRegistry();
        this.sentinelMetrics = new SentinelMetrics();
    }

    @AfterEach
    void tearDown() {
        this.registry.close();
        clear();
    }


    void clear() {
        clearEntryCallback();
        clearExitCallback();
    }

    @Test
    void testSupports() {
        assertFalse(this.sentinelMetrics.supports(this.registry));
        assertDoesNotThrow(SentinelNodeEventPublisherInitFuncTest::init);
        assertTrue(this.sentinelMetrics.supports(this.registry));
    }

    @Test
    void testDoBindTo() {
        assertDoesNotThrow(() -> this.sentinelMetrics.bindTo(this.registry));
        assertNull(this.sentinelMetrics.registry);
        assertDoesNotThrow(SentinelNodeEventPublisherInitFuncTest::init);
        assertDoesNotThrow(() -> this.sentinelMetrics.bindTo(this.registry));
        assertNotNull(this.sentinelMetrics.registry);
    }

    @Test
    void testOnEvent() {
        assertDoesNotThrow(SentinelNodeEventPublisherInitFuncTest::init);
        SentinelTemplate sntinelTemplate = new SentinelTemplate();
        this.sentinelMetrics.bindTo(this.registry);
        String resourceName = "test-resource";
        for (int i = 0; i < 100; i++) {
            sntinelTemplate.execute(resourceName, () -> {
            });
        }
        assertEquals(12, this.registry.getMeters().size());
    }
}