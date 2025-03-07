package io.microsphere.logging.logback.jmx;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LogbackPlatformLoggingMXBean} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since LogbackPlatformLoggingMXBean
 */
class LogbackPlatformLoggingMXBeanTest {

    private LogbackPlatformLoggingMXBean mxBean;

    @BeforeEach
    public void init() {
        LoggerFactory.getLogger(getLoggerName());
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(ROOT_LOGGER_NAME);
        this.mxBean = new LogbackPlatformLoggingMXBean(logger);
    }

    @Test
    void testGetLoggerNames() {
        assertFalse(mxBean.getLoggerNames().isEmpty());
        assertTrue(mxBean.getLoggerNames().contains(getLoggerName()));
    }

    @Test
    void testGetLoggerLevel() {
        assertEquals("TRACE", mxBean.getLoggerLevel(getLoggerName()));
    }

    @Test
    void testSetLoggerLevel() {
        mxBean.setLoggerLevel(getLoggerName(), "DEBUG");
        assertEquals("DEBUG", mxBean.getLoggerLevel(getLoggerName()));
    }

    @Test
    void testGetParentLoggerName() {
    }

    private String getLoggerName() {
        return getClass().getName();
    }
}