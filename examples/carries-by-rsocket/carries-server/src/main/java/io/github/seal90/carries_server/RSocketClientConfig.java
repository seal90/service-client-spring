package io.github.seal90.carries_server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeTypeUtils;

@Configuration
public class RSocketClientConfig {

    @Bean
    public RSocketRequester rSocketRequester(RSocketRequester.Builder builder) {
        return builder
                .tcp("localhost", 10000);
    }

}