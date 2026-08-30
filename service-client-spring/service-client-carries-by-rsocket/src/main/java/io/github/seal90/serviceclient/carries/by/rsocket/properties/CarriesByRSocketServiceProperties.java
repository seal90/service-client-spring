package io.github.seal90.serviceclient.carries.by.rsocket.properties;

import lombok.Data;

@Data
public class CarriesByRSocketServiceProperties {

  private String channelName;

  private String[] rSocketMessageHandlerBeanNames;

  private CarriesByRSocketChannelProperties channelConfig;

}
