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
package io.microsphere.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;

import java.util.Comparator;

/**
 * The {@link Comparator} for {@link LogEvent} based on logging time
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see LogEvent#getTimeMillis()
 * @since 1.0.0
 */
public class LogEventComparator implements Comparator<LogEvent> {

    /**
     * Singleton instance
     */
    public static final LogEventComparator INSTANCE = new LogEventComparator();

    private LogEventComparator() {
    }

    @Override
    public int compare(LogEvent o1, LogEvent o2) {
        return Long.compare(o1.getTimeMillis(), o2.getTimeMillis());
    }
}
