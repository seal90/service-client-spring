package io.github.seal90.serviceclient;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "seal.spring.service-client")
public class ServiceClientProperties {

  private String defaultProtocol = ProtocolType.GRPC;

}
