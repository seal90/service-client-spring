package io.github.seal90.serviceclient.protocoltypefactory.grpc.extension.metadataforwarding;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

import static io.github.seal90.serviceclient.protocoltypefactory.grpc.extension.metadataforwarding.ForwardGrpcMetadataServerInterceptor.METADATA_CTX_KEY;

@Data
@AllArgsConstructor
public class ForwardGrpcMetadataClientInterceptor implements ClientInterceptor {

  private String[] forwardGrpcMetadata;

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(methodDescriptor, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        if(forwardGrpcMetadata == null || forwardGrpcMetadata.length == 0) {
          super.start(responseListener, headers);
          return;
        }
        Metadata serverMetadata = METADATA_CTX_KEY.get();
        if(serverMetadata == null) {
          super.start(responseListener, headers);
          return;
        }
        for(String forward : forwardGrpcMetadata) {
          Metadata.Key<String> key = Metadata.Key.of(forward, Metadata.ASCII_STRING_MARSHALLER);
          if(!headers.containsKey(key) && serverMetadata.containsKey(key)) {
            headers.put(key, Objects.requireNonNull(serverMetadata.get(key)));
          }
        }

        super.start(responseListener, headers);
      }
    };
  }

}
