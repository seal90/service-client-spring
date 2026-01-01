package io.github.seal90.serviceclient.mqtt.eclipse;

import io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory.ProtocolTypeMqttEclipseFactory;
import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;

/**
 * ServiceClient configuration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ProtocolTypeMqttEclipseProperties.class})
public class ServiceClientMqttEclipseConfiguration {

  @Bean
  @ConditionalOnClass({MessageChannel.class, IMqttClient.class})
  public ProtocolTypeMqttEclipseFactory protocolTypeMqttEclipseFactory() {
    return new ProtocolTypeMqttEclipseFactory();
  }
}
