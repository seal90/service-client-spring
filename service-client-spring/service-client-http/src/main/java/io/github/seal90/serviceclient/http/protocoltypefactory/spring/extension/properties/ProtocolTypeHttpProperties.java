package io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.properties;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProtocolTypeHttpProperties {

  private String[] forwardMetadata;

  private String defaultChannelName;

  private Map<String, ProtocolTypeHttpChannelProperties> channels = new HashMap<>();

  private Map<String, ProtocolTypeHttpServiceProperties> services = new HashMap<>();

  public String addressByChannel(String channelName) {
    ProtocolTypeHttpChannelProperties channelProperties = channels.get(channelName);
    if(channelProperties == null) {
      return null;
    }
    return channelProperties.getAddress();
  }

  public String addressByServiceName(String serviceName) {
    ProtocolTypeHttpServiceProperties serviceProperties = services.get(serviceName);
    if(serviceProperties == null) {
      return null;
    }

    ProtocolTypeHttpChannelProperties channelProperties = serviceProperties.getChannelConfig();
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
