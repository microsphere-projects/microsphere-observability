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

package io.microsphere.observability.logging.spring.boot.autoconfigure;


import io.microsphere.logging.test.jupiter.LoggingLevelsClass;
import io.microsphere.spring.boot.test.WebAutoConfigurationTest;
import io.microsphere.spring.web.method.support.HandlerMethodInterceptor;
import io.microsphere.spring.webmvc.annotation.EnableWebMvcExtension;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.Set;

/**
 * {@link WebMvcLoggingAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebMvcLoggingAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                WebMvcLoggingAutoConfiguration.class,
                WebMvcLoggingAutoConfigurationTest.class
        }
)
@LoggingLevelsClass(
        loggingClasses = {
                WebMvcLoggingAutoConfiguration.class
        },
        levels = {
                "TRACE",
                "INFO",
                "ERROR"
        }
)
class WebMvcLoggingAutoConfigurationTest extends WebAutoConfigurationTest<WebMvcLoggingAutoConfiguration> {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    @Override
    protected void testAutoConfiguredClasses() {
        super.testAutoConfiguredClasses();
        ServletRequestHandledEvent event = new ServletRequestHandledEvent(this, "test", "127.0.0.1", "GET", "servlet", null, null, 1000);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    protected void configureAutoConfiguredClasses(Set<Class<?>> autoConfiguredClasses) {
    }

    @Override
    protected void configureGlobalDisabledPropertyValues(Set<String> globalDisabledPropertyValues) {
        globalDisabledPropertyValues.add("microsphere.spring.boot.webmvc.enabled=false");
    }

    @Override
    protected void configureGlobalMissingClasses(Set<Class<?>> globalMissingClasses) {
        globalMissingClasses.add(HandlerMethod.class);
        globalMissingClasses.add(DispatcherServlet.class);
        globalMissingClasses.add(HandlerMethodInterceptor.class);
        globalMissingClasses.add(EnableWebMvcExtension.class);
    }
}