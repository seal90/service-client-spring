package io.github.seal90.carries_server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seal90.serviceclient.carries.http.extension.CarriesMetadata;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
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