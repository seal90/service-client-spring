package io.github.seal90.serviceclient.carries.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seal90.serviceclient.carries.http.extension.CarriesMetadata;
import io.github.seal90.serviceclient.carries.http.properties.CarriesHttpByRSocketProperties;
import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeTypeUtils;

/**
 * ServiceClient configuration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CarriesHttpByRSocketProperties.class})
public class CarriesHttpConfiguration {

  @Bean
  public ProtocolTypeFactory carriesHttpFactory(ApplicationContext applicationContext, Environment environment
          , CarriesHttpByRSocketProperties carriesHttpByRSocketProperties, RSocketRequester.Builder rsocketRequesterBuilder) {
    return new CarriesHttpProtocolTypeFactory(applicationContext, environment, carriesHttpByRSocketProperties, rsocketRequesterBuilder);
  }

  @Bean
  public RSocketStrategiesCustomizer carriesHttpMetadata(ObjectMapper objectMapper) {
    return (strategy) -> {
      strategy.metadataExtractorRegistry(registry -> {
        registry.metadataToExtract(MimeTypeUtils.APPLICATION_JSON, CarriesMetadata.class, CarriesMetadata.CARRIES_METADATA_KEY);
      });
    };
  }
}
