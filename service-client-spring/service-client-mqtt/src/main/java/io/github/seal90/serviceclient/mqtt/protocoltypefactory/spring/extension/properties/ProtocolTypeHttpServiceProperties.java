package io.github.seal90.serviceclient.mqtt.protocoltypefactory.spring.extension.properties;

import lombok.Data;

@Data
public class ProtocolTypeHttpServiceProperties {

  private String channelName;

  private ProtocolTypeHttpChannelProperties channelConfig;
}
