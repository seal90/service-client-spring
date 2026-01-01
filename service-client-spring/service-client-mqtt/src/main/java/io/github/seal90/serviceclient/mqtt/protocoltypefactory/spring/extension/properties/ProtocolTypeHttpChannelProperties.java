package io.github.seal90.serviceclient.mqtt.protocoltypefactory.spring.extension.properties;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtocolTypeHttpChannelProperties {

  private String address;

  private List<String> addresses = new ArrayList<>();
}
