package io.github.seal90.serviceclient.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seal.spring.service-client.http")
public class ProtocolTypeHttpProperties extends io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.properties.ProtocolTypeHttpProperties {

}
