package io.github.seal90.serviceclient.carries.by.grpc.protocoltypefactory.grpc.extension.properties;

import lombok.Data;

@Data
public class ProtocolTypeGrpcServiceProperties {

  private String channelName;

  private ProtocolTypeGrpcChannelProperties channelConfig;

}
