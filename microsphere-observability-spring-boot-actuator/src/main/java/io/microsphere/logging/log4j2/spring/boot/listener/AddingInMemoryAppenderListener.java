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
import io.microsphere.spring.boot.context.OnceApplicationPreparedEventListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The {@link SpringApplication} {@link ApplicationListener Listener} to adding {@link InMemoryAppender}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see InMemoryAppender
 * @see OnceApplicationPreparedEventListener
 * @see ApplicationEnvironmentPreparedEvent
 * @since 1.0.0
 */
public class AddingInMemoryAppenderListener extends OnceApplicationPreparedEventListener {

    @Override
    protected void onApplicationEvent(SpringApplication springApplication, String[] args, ConfigurableApplicationContext context) {
        initializeInMemoryAppender();
    }

    private void initializeInMemoryAppender() {
        InMemoryAppender inMemoryAppender = new InMemoryAppender();
        Log4j2Utils.addAppenderForAllLoggers(inMemoryAppender);
    }

}

