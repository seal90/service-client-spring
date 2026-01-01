package io.github.seal90.serviceclient.mqtt.eclipse;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "seal.spring.service-client.mqtt-eclipse")
public class ProtocolTypeMqttEclipseProperties {

  private String[] forwardMetadata;

  private String defaultChannelName;

  private Map<String, ProtocolTypeMqttEclipseChannelProperties> channels = new HashMap<>();

  private Map<String, ProtocolTypeMqttEclipseServiceProperties> services = new HashMap<>();

  public ProtocolTypeMqttEclipseChannelProperties configByChannel(String channelName) {
    return channels.get(channelName);
  }

  public ProtocolTypeMqttEclipseChannelProperties configByServiceName(String serviceName) {
    ProtocolTypeMqttEclipseServiceProperties serviceProperties = services.get(serviceName);
    if(serviceProperties == null) {
      return null;
    }

    ProtocolTypeMqttEclipseChannelProperties channelProperties = serviceProperties.getChannelConfig();
    if(channelProperties == null) {
      String channelName = serviceProperties.getChannelName();
      return configByChannel(channelName);
    }
    return channelProperties;
  }

  public ProtocolTypeMqttEclipseChannelProperties configByDefault() {
    if(this.defaultChannelName == null) {
      return null;
    }
    return configByChannel(defaultChannelName);
  }

}
