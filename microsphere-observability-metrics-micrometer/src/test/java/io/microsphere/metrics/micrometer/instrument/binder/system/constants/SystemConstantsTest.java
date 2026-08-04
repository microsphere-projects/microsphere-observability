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

package io.microsphere.metrics.micrometer.instrument.binder.system.constants;


import org.junit.jupiter.api.Test;

import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.CGROUP_DIRECTORY_PATH_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.CGROUP_PREFIX;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.DEFAULT_NETWORK_STATS_FILE_PATH;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.METRICS_COLLECTION_INTERVAL_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.NETWORK_PREFIX;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.NETWORK_STATS_FILE_PATH_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link SystemConstants} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SystemConstants
 * @since 1.0.0
 */
class SystemConstantsTest {

    @Test
    void testConstants() {
        assertEquals("system.", PREFIX);
        assertEquals("system.cgroup.", CGROUP_PREFIX);
        assertEquals("/sys/fs/cgroup/", DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE);
        assertEquals("system.cgroup.dir", CGROUP_DIRECTORY_PATH_PROPERTY_NAME);

        assertEquals("/proc/net/dev", DEFAULT_NETWORK_STATS_FILE_PATH);
        assertEquals("system.network.", NETWORK_PREFIX);
        assertEquals("system.network.stats.file", NETWORK_STATS_FILE_PATH_PROPERTY_NAME);

        assertEquals("60000", DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE);
        assertEquals("system.metrics.micrometer.collection.interval", METRICS_COLLECTION_INTERVAL_PROPERTY_NAME);
    }
}