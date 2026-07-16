package io.microsphere.observability.logging.spring.boot.autoconfigure;

import io.microsphere.logging.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationListener;

import static io.microsphere.logging.LoggerFactory.getLogger;

/**
 * {@link WebServer} Auto-Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
@ConditionalOnWebApplication
public class WebServerLoggingAutoConfiguration implements ApplicationListener<WebServerInitializedEvent> {

    private static final Logger logger = getLogger(WebServerLoggingAutoConfiguration.class);

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        if (logger.isTraceEnabled()) {
            WebServer webServer = event.getWebServer();
            WebServerApplicationContext context = event.getApplicationContext();
            logger.trace("WebServer['{}' , context : '{}'] port : {}", webServer, context.getId(), webServer.getPort());
        }
    }
}