package io.microsphere.logging.spring.boot.autoconfigure;

import io.microsphere.logging.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.event.EventListener;
import org.springframework.web.context.support.ServletRequestHandledEvent;

import static io.microsphere.logging.LoggerFactory.getLogger;

/**
 * Web MVC Auto-Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcLoggingAutoConfiguration {

    private static final Logger logger = getLogger(WebMvcLoggingAutoConfiguration.class);

    @EventListener(ServletRequestHandledEvent.class)
    public void onServletRequestHandledEvent(ServletRequestHandledEvent event) {
        logger.debug("{}", event);
    }

}
