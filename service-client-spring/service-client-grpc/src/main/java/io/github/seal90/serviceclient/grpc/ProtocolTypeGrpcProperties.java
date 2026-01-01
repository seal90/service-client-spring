package io.github.seal90.serviceclient.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seal.spring.service-client.grpc")
public class ProtocolTypeGrpcProperties extends io.github.seal90.serviceclient.grpc.protocoltypefactory.grpc.extension.properties.ProtocolTypeGrpcProperties {
}
