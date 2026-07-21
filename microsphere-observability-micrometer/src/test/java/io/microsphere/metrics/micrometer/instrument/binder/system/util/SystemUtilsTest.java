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

package io.microsphere.metrics.micrometer.instrument.binder.system.util;


import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.CGROUP_DIRECTORY_PATH_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.DEFAULT_NETWORK_STATS_FILE_PATH;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.METRICS_COLLECTION_INTERVAL_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.NETWORK_STATS_FILE_PATH_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.util.SystemUtils.getCGroupDirectoryPath;
import static io.microsphere.metrics.micrometer.instrument.binder.system.util.SystemUtils.getMetricsCollectionInterval;
import static io.microsphere.metrics.micrometer.instrument.binder.system.util.SystemUtils.getNetworkStatsFilePath;
import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;
import static java.lang.System.getProperties;
import static java.lang.System.setProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link SystemUtils} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SystemUtils
 * @since 1.0.0
 */
class SystemUtilsTest {

    @Test
    void testGetCGroupDirectoryPath() {
        assertSame(DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE, getCGroupDirectoryPath());
        assertSystemPropertyValue(CGROUP_DIRECTORY_PATH_PROPERTY_NAME, "test_path", SystemUtils::getCGroupDirectoryPath);
    }

    @Test
    void testGetNetworkStatsFilePath() {
        assertSame(DEFAULT_NETWORK_STATS_FILE_PATH, getNetworkStatsFilePath());
        assertSystemPropertyValue(NETWORK_STATS_FILE_PATH_PROPERTY_NAME, "test_path", SystemUtils::getNetworkStatsFilePath);
    }

    @Test
    void testGetMetricsCollectionInterval() {
        assertEquals(parseLong(DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE), getMetricsCollectionInterval());
        assertSystemPropertyValue(METRICS_COLLECTION_INTERVAL_PROPERTY_NAME, 1000, SystemUtils::getMetricsCollectionInterval);
    }

    <V> void assertSystemPropertyValue(String name, V value, Supplier<V> propertyValueSupplier) {
        setProperty(name, valueOf(value));
        assertEquals(valueOf(value), valueOf(propertyValueSupplier.get()));
        getProperties().remove(name);
    }
}