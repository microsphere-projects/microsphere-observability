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

import javax.management.ObjectName;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.PlatformManagedObject;

import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.PlatformMXBeanCondition.isPlatformMXBeanAvailable;
import static io.microsphere.spring.beans.BeanUtils.isBeanPresent;
import static io.microsphere.spring.test.util.SpringTestUtils.testInSpringContainer;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnPlatformMXBeanAvailable} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnPlatformMXBeanAvailable
 * @since 1.0.0
 */
class ConditionalOnPlatformMXBeanAvailableTest {

    @Test
    void test() {
        testInSpringContainer(context -> {
            assertFalse(isBeanPresent(context, DefaultConfig.class));
        }, DefaultConfig.class);

        testInSpringContainer(context -> {
            assertTrue(isBeanPresent(context, ClassConfig.class));
        }, ClassConfig.class);

        testInSpringContainer(context -> {
            assertTrue(isBeanPresent(context, ClassNameConfig.class));
        }, ClassNameConfig.class);

        testInSpringContainer(context -> {
            assertFalse(isBeanPresent(context, ClassNotFoundConfig.class));
        }, ClassNotFoundConfig.class);

        testInSpringContainer(context -> {
            assertFalse(isBeanPresent(context, PlatformManagedTypeConfig.class));
        }, PlatformManagedTypeConfig.class);

        testInSpringContainer(context -> {
            assertFalse(isBeanPresent(context, NonPlatformManagedObjectConfig.class));
        }, NonPlatformManagedObjectConfig.class);
    }

    @Test
    void testIsPlatformMXBeanAvailable() {
        assertTrue(isPlatformMXBeanAvailable(ClassLoadingMXBean.class));
        assertFalse(isPlatformMXBeanAvailable(PlatformManagedType.class));
        assertFalse(isPlatformMXBeanAvailable(Object.class));
    }

    static class PlatformManagedType implements PlatformManagedObject {

        @Override
        public ObjectName getObjectName() {
            return null;
        }
    }

    @ConditionalOnPlatformMXBeanAvailable
    static class DefaultConfig {
    }

    @ConditionalOnPlatformMXBeanAvailable(
            value = {
                    ClassLoadingMXBean.class,
                    CompilationMXBean.class,
                    OperatingSystemMXBean.class
            }
    )
    static class ClassConfig {
    }

    @ConditionalOnPlatformMXBeanAvailable(
            type = {
                    "java.lang.management.ClassLoadingMXBean",
                    "java.lang.management.CompilationMXBean",
                    "com.sun.management.OperatingSystemMXBean"
            }
    )
    static class ClassNameConfig {
    }

    @ConditionalOnPlatformMXBeanAvailable(
            type = {
                    "not-found-class"
            }
    )
    static class ClassNotFoundConfig {
    }

    @ConditionalOnPlatformMXBeanAvailable(
            value = {
                    PlatformManagedType.class
            }
    )
    static class PlatformManagedTypeConfig {
    }

    @ConditionalOnPlatformMXBeanAvailable(
            type = {
                    "java.lang.Object"
            }
    )
    static class NonPlatformManagedObjectConfig {
    }
}