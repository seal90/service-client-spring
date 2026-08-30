package io.github.seal90.serviceclient.carries.by.grpc.protocoltypefactory.grpc.extension.properties;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProtocolTypeGrpcProperties {

  private String[] forwardMetadata;

  private String defaultChannelName;

  private Map<String, ProtocolTypeGrpcChannelProperties> channels = new HashMap<>();

  private Map<String, ProtocolTypeGrpcServiceProperties> services = new HashMap<>();

  public String addressByChannel(String channelName) {
    ProtocolTypeGrpcChannelProperties channelProperties = channels.get(channelName);
    if(channelProperties == null) {
      return null;
    }
    return channelProperties.getAddress();
  }

  public String addressByServiceName(String serviceName) {
    ProtocolTypeGrpcServiceProperties serviceProperties = services.get(serviceName);
    if(serviceProperties == null) {
      return null;
    }

    ProtocolTypeGrpcChannelProperties channelProperties = serviceProperties.getChannelConfig();
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
