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
package io.microsphere.logging.log4j2.appender;

import io.microsphere.logging.log4j2.LogEventComparator;
import io.microsphere.logging.log4j2.util.Log4j2Utils;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * In-Memory {@link Appender} records any {@link LogEvent}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Appender
 * @since 1.0.0
 */
public class InMemoryAppender extends AbstractLifeCycle implements Appender {

    /**
     * The name of {@link InMemoryAppender}
     */
    public static final String NAME = "InMemory";

    private final Set<LogEvent> logEvents = new ConcurrentSkipListSet<>(LogEventComparator.INSTANCE);

    @Override
    public void append(LogEvent event) {
        logEvents.add(event);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Layout<? extends Serializable> getLayout() {
        return null;
    }

    @Override
    public boolean ignoreExceptions() {
        return false;
    }

    @Override
    public ErrorHandler getHandler() {
        return null;
    }

    @Override
    public void setHandler(ErrorHandler handler) {
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        this.logEvents.clear();
    }

    /**
     * Transfer all log events to another {@link Appender}
     *
     * @param appender another {@link Appender}
     */
    public void transfer(Appender appender) {
        Iterator<LogEvent> iterator = logEvents.iterator();
        while (iterator.hasNext()) {
            LogEvent logEvent = iterator.next();
            appender.append(logEvent);
            iterator.remove();
        }
    }

    public static InMemoryAppender findInMemoryAppender() {
        return Log4j2Utils.findAppender(NAME);
    }
}
