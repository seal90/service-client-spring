package io.github.seal90.carries_server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seal90.serviceclient.carries.http.extension.CarriesMetadata;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MimeTypeUtils;

@Configuration
public class ServerRSocketConfig {

    @Bean
    public RSocketStrategiesCustomizer httpProxyHeader(ObjectMapper objectMapper) {
        return (strategy) -> {
            strategy.metadataExtractorRegistry(registry -> {
                registry.metadataToExtract(MimeTypeUtils.APPLICATION_JSON, CarriesMetadata.class, CarriesMetadata.CARRIES_METADATA_KEY);
            });
        };
    }

}
