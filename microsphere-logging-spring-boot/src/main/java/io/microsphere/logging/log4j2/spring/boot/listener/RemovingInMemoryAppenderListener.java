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
package io.microsphere.logging.log4j2.spring.boot.listener;

import io.microsphere.logging.log4j2.appender.InMemoryAppender;
import io.microsphere.logging.log4j2.util.Log4j2Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

import static io.microsphere.logging.log4j2.appender.InMemoryAppender.findInMemoryAppender;

/**
 * The {@link SpringApplication} {@link ApplicationListener Listener} for removing {@link InMemoryAppender}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see InMemoryAppender
 * @see ApplicationListener
 * @see ApplicationStartedEvent
 * @since 1.0.0
 */
public class RemovingInMemoryAppenderListener implements ApplicationListener<ApplicationStartedEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        removeInMemoryAppender();
    }

    private void removeInMemoryAppender() {
        InMemoryAppender inMemoryAppender = findInMemoryAppender();
        Log4j2Utils.removeAppenderForAllLoggers(inMemoryAppender);
    }

}

