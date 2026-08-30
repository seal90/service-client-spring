package io.github.seal90.serviceclient.carries.by.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seal.spring.service-client.carries-by-grpc")
public class ProtocolTypeCarriesByGrpcProperties extends io.github.seal90.serviceclient.carries.by.grpc.protocoltypefactory.grpc.extension.properties.ProtocolTypeGrpcProperties {
}
