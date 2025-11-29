package io.github.seal90.grpc_server;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class GrpcServerConfiguration {

  @Bean
  @GlobalServerInterceptor
  public ServerInterceptor serverInterceptor() {

    return new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                   Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String value = metadata.get(Metadata.Key.of("CLIENT_TO_SERVER_HEADER_KEY", Metadata.ASCII_STRING_MARSHALLER));
        log.info("--- server receive client header CLIENT_TO_SERVER_HEADER_KEY : {}", value);
        String overlyNS = metadata.get(Metadata.Key.of("overlay-ns", Metadata.ASCII_STRING_MARSHALLER));
        log.info("--- server receive client header overlay-ns : {}", overlyNS);

        ServerCall<ReqT, RespT> newServerCall = new ForwardingServerCall.SimpleForwardingServerCall<>(
            serverCall) {

          @Override
          public void sendHeaders(Metadata headers) {
            // If necessary, values can be passed to the client.
            headers.put(Metadata.Key.of("SERVER_TO_CLIENT_HEADER_KEY", Metadata.ASCII_STRING_MARSHALLER),
                "SERVER_TO_CLIENT_HEADER_VALUE");
            super.sendHeaders(headers);
          }
        };
        return serverCallHandler.startCall(newServerCall, metadata);
      }
    };
  }
}
