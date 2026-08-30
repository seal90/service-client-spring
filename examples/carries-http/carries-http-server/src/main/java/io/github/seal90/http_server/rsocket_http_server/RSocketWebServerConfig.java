package io.github.seal90.http_server.rsocket_http_server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.messaging.rsocket.RSocketStrategies;

@Configuration(proxyBeanMethods = false)
public class RSocketWebServerConfig {

//    @Bean
//    public RSocketReactiveWebServerFactory rSocketReactiveWebServerFactory(RSocketStrategies rsocketStrategies) {
//        return new RSocketReactiveWebServerFactory(rsocketStrategies);
//    }

    @Bean
    public RSocketWebServerSmartLifecycle rSocketWebServerSmartLifecycle(HttpHandler httpHandler, RSocketStrategies rsocketStrategies) {
        return new RSocketWebServerSmartLifecycle(httpHandler, rsocketStrategies);
    }
}
