package io.github.seal90.serviceclient.carries.by.rsocket.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "seal.spring.service-client.carries.by.rsocket")
public class CarriesByRSocketProperties {

  private String defaultChannelName;

  private Map<String, CarriesByRSocketChannelProperties> channels = new HashMap<>();

  private Map<String, CarriesByRSocketServiceProperties> services = new HashMap<>();

  public String addressByChannel(String channelName) {
    CarriesByRSocketChannelProperties channelProperties = channels.get(channelName);
    if(channelProperties == null) {
      return null;
    }
    return channelProperties.getAddress();
  }

  public String addressByServiceName(String serviceName) {
    CarriesByRSocketServiceProperties serviceProperties = services.get(serviceName);
    if(serviceProperties == null) {
      return null;
    }

    CarriesByRSocketChannelProperties channelProperties = serviceProperties.getChannelConfig();
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
