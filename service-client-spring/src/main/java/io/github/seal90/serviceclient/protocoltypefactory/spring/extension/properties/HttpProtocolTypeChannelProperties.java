package io.github.seal90.serviceclient.protocoltypefactory.spring.extension.properties;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HttpProtocolTypeChannelProperties {

  private String address;

  private List<String> addresses = new ArrayList<>();
}
