package io.github.seal90.serviceclient.grpc;

import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.github.seal90.serviceclient.grpc.protocoltypefactory.ProtocolTypeGrpcFactory;
import io.github.seal90.serviceclient.grpc.protocoltypefactory.grpc.extension.metadataforwarding.ForwardMetadataClientInterceptor;
import io.github.seal90.serviceclient.grpc.protocoltypefactory.grpc.extension.metadataforwarding.ForwardMetadataServerInterceptor;
import io.grpc.ClientInterceptor;
import io.grpc.Grpc;
import io.grpc.ServerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * ServiceClient configuration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ProtocolTypeGrpcProperties.class})
public class ServiceClientGrpcConfiguration {

  @ConditionalOnClass(Grpc.class)
  public static class GrpcConfig {

    @Bean
    public ProtocolTypeFactory grpcProtocolTypeFactory() {
      return new ProtocolTypeGrpcFactory();
    }

    @Bean
    @ServiceClientInterceptor
    public ClientInterceptor forwardMetadataClientInterceptor(ProtocolTypeGrpcProperties protocolTypeProperties) {
      return new ForwardMetadataClientInterceptor(protocolTypeProperties.getForwardMetadata());
    }

    @Bean
    @GlobalServerInterceptor
    @ConditionalOnClass(GlobalServerInterceptor.class)
    public ServerInterceptor forwardMetadataServerInterceptor() {
      return new ForwardMetadataServerInterceptor();
    }
  }
}
