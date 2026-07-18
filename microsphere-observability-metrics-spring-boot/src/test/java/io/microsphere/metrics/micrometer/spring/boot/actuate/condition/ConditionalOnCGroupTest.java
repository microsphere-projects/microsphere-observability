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

package io.microsphere.metrics.micrometer.spring.boot.actuate.condition;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.env.MockEnvironment;

import java.io.File;

import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.CGroupConstants.CGROUP_DIRECTORY_PATH_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.util.CGroupUtils.getCGroupDirectoryPath;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnCGroup.CGROUP_DIRECTORY_PLACEHOLDER;
import static io.microsphere.spring.beans.BeanUtils.isBeanPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link ConditionalOnCGroup} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ConditionalOnCGroup
 * @since 1.0.0
 */
public class ConditionalOnCGroupTest {

    @Test
    void test() {
        String path = "file:///sys/fs/cgroup/";
        MockEnvironment environment = new MockEnvironment();
        assertEquals(path, environment.resolvePlaceholders(CGROUP_DIRECTORY_PLACEHOLDER));

        path = "/var/";
        environment.setProperty(CGROUP_DIRECTORY_PATH_PROPERTY_NAME, path);
        assertEquals(path, environment.resolvePlaceholders(CGROUP_DIRECTORY_PLACEHOLDER));
    }

    @SpringBootTest(
            classes = {
                    DefaultTest.class
            },
            webEnvironment = NONE
    )
    @ConditionalOnCGroup
    static class DefaultTest {

        @Autowired
        private ListableBeanFactory beanFactory;

        @Test
        void test() {
            String cgroupDir = getCGroupDirectoryPath();
            File file = new File(cgroupDir);
            assertEquals(file.exists(), isBeanPresent(this.beanFactory, DefaultTest.class));
        }
    }

    @SpringBootTest(
            classes = {
                    SpecifiedTest.class
            },
            webEnvironment = NONE,
            properties = {
                    "cgroup.dir=file://${user.dir}"
            }
    )
    @ConditionalOnCGroup
    static class SpecifiedTest {

        @Autowired
        private ListableBeanFactory beanFactory;

        @Test
        void test() {
            assertTrue(isBeanPresent(this.beanFactory, SpecifiedTest.class));
        }
    }

    @SpringBootTest(
            classes = {
                    DisabledTest.class
            },
            webEnvironment = NONE,
            properties = {
                    "cgroup.dir=file://not-found"
            }
    )
    @ConditionalOnCGroup
    static class DisabledTest {

        @Autowired
        private ListableBeanFactory beanFactory;

        @Test
        void test() {
            assertFalse(isBeanPresent(this.beanFactory, DisabledTest.class));
        }
    }

}
