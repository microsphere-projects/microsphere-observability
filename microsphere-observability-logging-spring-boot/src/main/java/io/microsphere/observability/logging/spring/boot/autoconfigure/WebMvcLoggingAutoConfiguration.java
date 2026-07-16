package io.microsphere.observability.logging.spring.boot.autoconfigure;

import io.microsphere.logging.Logger;
import io.microsphere.spring.boot.webmvc.autoconfigure.condition.ConditionalOnWebMvcAvailable;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationListener;
import org.springframework.web.context.support.ServletRequestHandledEvent;

import static io.microsphere.logging.LoggerFactory.getLogger;

/**
 * Web MVC Auto-Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see org.springframework.web.servlet.FrameworkServlet
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
 * @since 1.0.0
 */
@ConditionalOnWebMvcAvailable
@AutoConfigureAfter(
        name = {
                "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration",       // Spring Boot Web API
                "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration"             // Spring Boot WebMVC 4.0+ API
        }
)
public class WebMvcLoggingAutoConfiguration implements ApplicationListener<ServletRequestHandledEvent> {

    private static final Logger logger = getLogger(WebMvcLoggingAutoConfiguration.class);

    @Override
    public void onApplicationEvent(ServletRequestHandledEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("{}", event);
        }
    }
}