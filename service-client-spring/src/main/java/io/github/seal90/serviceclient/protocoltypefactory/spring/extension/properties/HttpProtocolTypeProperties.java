package io.github.seal90.serviceclient.protocoltypefactory.spring.extension.properties;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpProtocolTypeProperties {

  private String[] forwardWebHeaders;

  private String defaultChannelName;

  private Map<String, HttpProtocolTypeChannelProperties> channels = new HashMap<>();

  private Map<String, HttpProtocolTypeServiceProperties> services = new HashMap<>();

}
