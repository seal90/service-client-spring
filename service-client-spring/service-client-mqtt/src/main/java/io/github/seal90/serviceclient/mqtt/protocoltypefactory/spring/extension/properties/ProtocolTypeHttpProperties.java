package io.github.seal90.serviceclient.mqtt.protocoltypefactory.spring.extension.properties;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProtocolTypeHttpProperties {

  private String[] forwardWebHeaders;

  private String defaultChannelName;

  private Map<String, ProtocolTypeHttpChannelProperties> channels = new HashMap<>();

  private Map<String, ProtocolTypeHttpServiceProperties> services = new HashMap<>();

}
