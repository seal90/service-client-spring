package io.github.seal90.serviceclient.carries.http.properties;

import lombok.Data;

@Data
public class CarriesHttpByRSocketServiceProperties {

  private String channelName;

  private String[] rSocketMessageHandlerBeanNames;

  private CarriesHttpByRSocketChannelProperties channelConfig;

}
