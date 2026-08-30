package io.github.seal90.http_server.rsocket_http_server;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.reactive.AbstractReactiveWebServerFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.messaging.rsocket.RSocketStrategies;

public class RSocketReactiveWebServerFactory extends AbstractReactiveWebServerFactory {
    private final RSocketStrategies rsocketStrategies;

    public RSocketReactiveWebServerFactory(RSocketStrategies rsocketStrategies) {
        this.rsocketStrategies = rsocketStrategies;
    }

    @Override
    public WebServer getWebServer(HttpHandler httpHandler) {
        RSocketWebServer rSocketWebServer = new RSocketWebServer(httpHandler, this.rsocketStrategies);

        return rSocketWebServer;
    }
}
