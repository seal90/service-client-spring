package io.github.seal90.serviceclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seal.spring.service-client.grpc")
public class GrpcProtocolTypeProperties extends io.github.seal90.serviceclient.protocoltypefactory.grpc.extension.properties.GrpcProtocolTypeProperties{
}
