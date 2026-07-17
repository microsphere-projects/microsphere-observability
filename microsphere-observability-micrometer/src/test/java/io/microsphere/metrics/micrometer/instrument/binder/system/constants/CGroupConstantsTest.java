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

import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.CGroupConstants.CGROUP_DIRECTORY;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.CGroupConstants.CGROUP_DIRECTORY_SYSTEM_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.CGroupConstants.DEFAULT_CGROUP_DIRECTORY_SYSTEM_PROPERTY_VALUE;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.CGroupConstants.PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link CGroupConstants} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see CGroupConstants
 * @since 1.0.0
 */
class CGroupConstantsTest {

    @Test
    void testConstants() {
        assertEquals("cgroup.", PREFIX);
        assertEquals("/sys/fs/cgroup/", DEFAULT_CGROUP_DIRECTORY_SYSTEM_PROPERTY_VALUE);
        assertEquals("cgroup.dir", CGROUP_DIRECTORY_SYSTEM_PROPERTY_NAME);
        assertNotNull(CGROUP_DIRECTORY);
    }
}