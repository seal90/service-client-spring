package io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.properties;

import lombok.Data;

@Data
public class ProtocolTypeHttpServiceProperties {

  private String channelName;

  private ProtocolTypeHttpChannelProperties channelConfig;
}
