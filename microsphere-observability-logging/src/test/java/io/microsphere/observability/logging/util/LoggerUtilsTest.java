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

package io.microsphere.observability.logging.util;


import org.junit.jupiter.api.Test;

import static io.microsphere.observability.logging.util.LoggerUtils.debug;
import static io.microsphere.observability.logging.util.LoggerUtils.error;
import static io.microsphere.observability.logging.util.LoggerUtils.info;
import static io.microsphere.observability.logging.util.LoggerUtils.log;
import static io.microsphere.observability.logging.util.LoggerUtils.trace;
import static io.microsphere.observability.logging.util.LoggerUtils.warn;

/**
 * {@link LoggerUtils} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see LoggerUtils
 * @since 1.0.0
 */
class LoggerUtilsTest {

    @Test
    void test() {
        trace(logger -> logger.trace("This is a trace message"));
        debug(logger -> logger.debug("This is a debug message"));
        info(logger -> logger.info("This is an info message"));
        warn(logger -> logger.warn("This is a warn message"));
        error(logger -> logger.error("This is an error message"));
        log(logger -> false, logger -> {
        });
    }
}