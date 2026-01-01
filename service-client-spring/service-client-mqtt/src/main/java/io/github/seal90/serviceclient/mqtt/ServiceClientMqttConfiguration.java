package io.github.seal90.serviceclient.mqtt;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ServiceClient configuration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ProtocolTypeMqttProperties.class})
public class ServiceClientMqttConfiguration {


}
