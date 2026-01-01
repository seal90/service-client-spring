package io.github.seal90.serviceclient.mqtt.eclipse;

import lombok.Data;

@Data
public class ProtocolTypeMqttEclipseChannelProperties {

  private String[] serverURIs;

  private String clientId;

  private String userName;

  private String password;

  private String defaultTopic;

}
