package io.github.seal90.serviceclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seal.spring.service-client.http")
public class HttpProtocolTypeProperties extends io.github.seal90.serviceclient.protocoltypefactory.spring.extension.properties.HttpProtocolTypeProperties {

}
