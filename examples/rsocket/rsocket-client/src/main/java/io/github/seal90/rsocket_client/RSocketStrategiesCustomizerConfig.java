package io.github.seal90.rsocket_client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MimeType;

import static io.github.seal90.rsocket_client.RSocketClientConfiguration.CLIENT_TO_SERVER_HEADER_VALUE_MIMETYPE;
import static io.github.seal90.rsocket_client.RSocketClientConfiguration.OVERLAY_NS_MIMETYPE;
import static io.github.seal90.rsocket_client.RSocketClientConfiguration.SERVER_TO_CLIENT_HEADER_KEY_MIMETYPE;

@Slf4j
@Configuration
public class RSocketStrategiesCustomizerConfig {

  @Bean
  public RSocketStrategiesCustomizer rSocketStrategiesCustomizer() {
    return (strategy) -> {
      strategy.metadataExtractorRegistry(registry -> {
        registry.metadataToExtract(MimeType.valueOf(CLIENT_TO_SERVER_HEADER_VALUE_MIMETYPE), String.class, "client_to_server_header_value");
        registry.metadataToExtract(MimeType.valueOf(OVERLAY_NS_MIMETYPE), String.class, "overlay_ns");
        registry.metadataToExtract(MimeType.valueOf(SERVER_TO_CLIENT_HEADER_KEY_MIMETYPE), String.class, "server_to_client_header_key");
      });
    };
  }

}
