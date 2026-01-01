package io.github.seal90.serviceclient.rsocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "seal.spring.service-client.rsocket")
public class ProtocolTypeRSocketProperties {

  private String[] forwardMetadata;

  private String defaultChannelName;

  private Map<String, ProtocolTypeRSocketChannelProperties> channels = new HashMap<>();

  private Map<String, ProtocolTypeRSocketServiceProperties> services = new HashMap<>();

  public String addressByChannel(String channelName) {
    ProtocolTypeRSocketChannelProperties channelProperties = channels.get(channelName);
    if(channelProperties == null) {
      return null;
    }
    return channelProperties.getAddress();
  }

  public String addressByServiceName(String serviceName) {
    ProtocolTypeRSocketServiceProperties serviceProperties = services.get(serviceName);
    if(serviceProperties == null) {
      return null;
    }

    ProtocolTypeRSocketChannelProperties channelProperties = serviceProperties.getChannelConfig();
    if(channelProperties == null) {
      String channelName = serviceProperties.getChannelName();
      return addressByChannel(channelName);
    }
    return channelProperties.getAddress();
  }

  public String addressByDefault() {
    if(this.defaultChannelName == null) {
      return null;
    }
    return addressByChannel(defaultChannelName);
  }

}
