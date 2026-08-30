package io.github.seal90.serviceclient.carries.by.grpc;

import io.github.seal90.serviceclient.carries.by.grpc.protocoltypefactory.CarriesByRSocketProtocolTypeTransactionFactory;
import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.github.seal90.serviceclient.carries.by.grpc.protocoltypefactory.ProtocolTypeCarriesByGrpcFactory;
import io.github.seal90.serviceclient.carries.by.grpc.protocoltypefactory.grpc.extension.metadataforwarding.ForwardMetadataClientInterceptor;
import io.github.seal90.serviceclient.carries.by.grpc.protocoltypefactory.grpc.extension.metadataforwarding.ForwardMetadataServerInterceptor;
import io.grpc.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * ServiceClient configuration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ProtocolTypeCarriesByGrpcProperties.class})
public class ServiceClientCarriesByGrpcConfiguration {

  @ConditionalOnClass(Grpc.class)
  public static class GrpcConfig {

    @Bean
    public ProtocolTypeFactory carriesByGrpcProtocolTypeFactory() {
      return new ProtocolTypeCarriesByGrpcFactory();
    }

    @Bean
    public ProtocolTypeFactory carriesByRSocketProtocolTypeTransactionFactory() {
      return new CarriesByRSocketProtocolTypeTransactionFactory();
    }

    @Bean
    @ServiceClientInterceptor
    public ClientInterceptor forwardCarriesByGrpcMetadataClientInterceptor(ProtocolTypeCarriesByGrpcProperties protocolTypeProperties) {
      return new ForwardMetadataClientInterceptor(protocolTypeProperties.getForwardMetadata());
    }

    @Bean
    @GlobalServerInterceptor
    @ConditionalOnClass(GlobalServerInterceptor.class)
    public ServerInterceptor forwardCarriesByGrpcMetadataServerInterceptor() {
      return new ForwardMetadataServerInterceptor();
    }

  }
}
