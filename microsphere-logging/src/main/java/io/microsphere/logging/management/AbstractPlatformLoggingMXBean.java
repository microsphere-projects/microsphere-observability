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
package io.microsphere.logging.management;

import javax.management.ObjectName;
import java.lang.management.PlatformLoggingMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Hashtable;
import java.util.StringJoiner;


import static java.util.Collections.emptyMap;

/**
 * Abstract {@link PlatformLoggingMXBean} Class
 *
 * @param <L> the type of Logger
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see PlatformLoggingMXBean
 * @since 1.0.0
 */
public abstract class AbstractPlatformLoggingMXBean<L> implements PlatformLoggingMXBean {

    protected final L logger;

    protected final ObjectName objectName;

    protected AbstractPlatformLoggingMXBean(L logger) {
        this(logger, emptyMap());
    }

    protected AbstractPlatformLoggingMXBean(L logger, Map<String, String> properties) {
        this.logger = logger;
        this.objectName = buildObjectName(logger, properties);
    }

    private ObjectName buildObjectName(L logger, Map<String, String> properties) {
        Class<?> loggerClass = logger.getClass();
        String domain = loggerClass.getPackage().getName();
        String type = loggerClass.getSimpleName();
        Map<String, String> propertyList = new HashMap<>(properties.size() + 1);
        propertyList.put("type", type);
        propertyList.putAll(properties);
        try {
            return new ObjectName(domain, new Hashtable<>(propertyList));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    @Override
    public ObjectName getObjectName() {
        return objectName;
    }
}
