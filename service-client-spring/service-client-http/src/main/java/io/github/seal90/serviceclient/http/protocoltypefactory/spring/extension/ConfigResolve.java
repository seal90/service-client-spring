package io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension;

import io.github.seal90.serviceclient.core.ChannelNamePrefix;
import io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.properties.ProtocolTypeHttpProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.env.Environment;

class ConfigResolve {

  public static NameResolveResult findAddress(String serviceName, String channelName
      , Environment environment, ProtocolTypeHttpProperties properties) {
    String address;
    boolean resolved = true;
    if(!channelName.isEmpty()) {
      if (ChannelNamePrefix.isStatic(channelName)) {
        address = ChannelNamePrefix.extractStatic(channelName);
        address = environment.resolveRequiredPlaceholders(address);
      } else if (ChannelNamePrefix.isChannel(channelName)) {
        address = properties.addressByChannel(ChannelNamePrefix.extractChannel(channelName));
      } else if (ChannelNamePrefix.isDefault(channelName)) {
        address = properties.addressByDefault();
      } else if (ChannelNamePrefix.isLb(channelName)) {
        address = "http://" + ChannelNamePrefix.extractLb(channelName);
        resolved = false;
      } else {
        // default is channel name
        address = properties.addressByChannel(channelName);
      }
    } else {
      address = properties.addressByServiceName(serviceName);
      if(address == null) {
        address = "http://" + serviceName;
        resolved = false;
      }
    }
    return new NameResolveResult(address, resolved);
  }

  @Data
  @AllArgsConstructor
  public static class NameResolveResult {
    String address;
    Boolean resolved;
  }
}
