package io.github.seal90.serviceclient.http.feign;

import feign.Feign;
import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.http.feign.protocoltypefactory.ProtocolTypeHttpFeignFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ServiceClient configuration
 */
@Configuration(proxyBeanMethods = false)
public class ServiceClientHttpFeignConfiguration {

  @Bean
  @ConditionalOnClass(Feign.class)
  public ProtocolTypeFactory protocolTypeHttpFeignFactory() {
    return new ProtocolTypeHttpFeignFactory();
  }

}
