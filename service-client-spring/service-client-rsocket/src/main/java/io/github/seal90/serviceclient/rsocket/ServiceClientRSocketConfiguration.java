package io.github.seal90.serviceclient.rsocket;

import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.rsocket.protocoltypefactory.ProtocolTypeRSocketFactory;
import io.github.seal90.serviceclient.rsocket.protocoltypefactory.client.RSocketClientRegistryContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ServiceClient configuration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ProtocolTypeRSocketProperties.class})
public class ServiceClientRSocketConfiguration {

  @Bean
  @ConditionalOnMissingBean(ProtocolTypeRSocketFactory.class)
  public ProtocolTypeFactory protocolTypeRSocketFactory(ApplicationContext applicationContext) {
    return new ProtocolTypeRSocketFactory();
  }

  @Bean
  public RSocketClientRegistryContext rSocketClientRegistryContext() {
    return new RSocketClientRegistryContext();
  }

}
