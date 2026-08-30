package io.github.seal90.serviceclient.carries.http.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "seal.spring.service-client.carries.rsocket")
public class CarriesHttpByRSocketProperties {

  private String defaultChannelName;

  private Map<String, CarriesHttpByRSocketChannelProperties> channels = new HashMap<>();

  private Map<String, CarriesHttpByRSocketServiceProperties> services = new HashMap<>();

  public String addressByChannel(String channelName) {
    CarriesHttpByRSocketChannelProperties channelProperties = channels.get(channelName);
    if(channelProperties == null) {
      return null;
    }
    return channelProperties.getAddress();
  }

  public String addressByServiceName(String serviceName) {
    CarriesHttpByRSocketServiceProperties serviceProperties = services.get(serviceName);
    if(serviceProperties == null) {
      return null;
    }

    CarriesHttpByRSocketChannelProperties channelProperties = serviceProperties.getChannelConfig();
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
