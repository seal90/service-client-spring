package io.github.seal90.serviceclient.mqtt.eclipse;

import lombok.Data;

@Data
public class ProtocolTypeMqttEclipseServiceProperties {

  private String channelName;

  private ProtocolTypeMqttEclipseChannelProperties channelConfig;

}
