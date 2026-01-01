package io.github.seal90.serviceclient.rsocket;

import lombok.Data;

@Data
public class ProtocolTypeRSocketServiceProperties {

  private String channelName;

  private String[] rSocketMessageHandlerBeanNames;

  private ProtocolTypeRSocketChannelProperties channelConfig;

}
