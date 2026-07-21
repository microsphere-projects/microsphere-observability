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

import io.microsphere.logging.Logger;
import io.microsphere.spring.core.annotation.ResolvablePlaceholderAnnotationAttributes;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.util.Map;

import static io.microsphere.lang.function.ThrowableSupplier.execute;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.spring.core.annotation.ResolvablePlaceholderAnnotationAttributes.of;
import static io.microsphere.text.FormatUtils.format;
import static io.microsphere.util.ArrayUtils.isEmpty;
import static io.microsphere.util.ClassLoaderUtils.resolveClass;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.management.ManagementFactory.getPlatformMXBean;
import static java.util.Objects.nonNull;
import static org.springframework.boot.autoconfigure.condition.ConditionOutcome.match;
import static org.springframework.boot.autoconfigure.condition.ConditionOutcome.noMatch;

/**
 * {@link SpringBootCondition Spring Boot Condition} for JMX PlatformMXBean available
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ConditionalOnPlatformMXBeanAvailable
 * @see SpringBootCondition
 * @see ManagementFactory#getPlatformMXBean(Class)
 * @since 1.0.0
 */
class PlatformMXBeanCondition extends SpringBootCondition {

    private static final Class<ConditionalOnPlatformMXBeanAvailable> annotationType = ConditionalOnPlatformMXBeanAvailable.class;

    private static final Logger logger = getLogger(annotationType);

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(annotationType.getName());
        ResolvablePlaceholderAnnotationAttributes attributes = of(annotationAttributes, annotationType, context.getEnvironment());
        Class<?>[] interfaceClasses = attributes.getClassArray("value");
        String[] interfaceClassNames = attributes.getStringArray("type");

        if (isEmpty(interfaceClasses) && isEmpty(interfaceClassNames)) {
            return noMatch("The annotation attributes[type : 'value' and type : 'type'] are empty");
        }

        for (Class<?> interfaceClass : interfaceClasses) {
            if (!isPlatformMXBeanAvailable(interfaceClass)) {
                return noMatch(format("The PlatformMXBean interface class[name : '{}'] is not available", interfaceClass.getName()));
            }
        }

        ClassLoader systemClassLoader = getSystemClassLoader();

        for (String interfaceClassName : interfaceClassNames) {
            Class<?> interfaceClass = resolveClass(interfaceClassName, systemClassLoader);
            if (interfaceClass == null) {
                return noMatch(format("The PlatformMXBean interface class[name : '{}'] can't be found in the System ClassLoader", interfaceClassName));
            }
            if (!isPlatformMXBeanAvailable(interfaceClass)) {
                return noMatch(format("The PlatformMXBean interface class[name : '{}'] is not available", interfaceClassName));
            }
        }

        return match();
    }

    static boolean isPlatformMXBeanAvailable(Class<?> interfaceClass) {
        if (!PlatformManagedObject.class.isAssignableFrom(interfaceClass)) {
            logger.warn("The PlatformMXBean interface[{}] is not a subtype of java.lang.management.PlatformManagedObject", interfaceClass);
            return false;
        }
        return execute(() -> nonNull(getPlatformMXBean((Class<PlatformManagedObject>) interfaceClass)), e -> false);
    }
}