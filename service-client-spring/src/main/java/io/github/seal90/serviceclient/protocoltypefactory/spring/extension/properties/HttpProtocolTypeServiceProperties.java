package io.github.seal90.serviceclient.protocoltypefactory.spring.extension.properties;

import lombok.Data;

@Data
public class HttpProtocolTypeServiceProperties {

  private String channelName;

  private HttpProtocolTypeChannelProperties channelConfig;
}
