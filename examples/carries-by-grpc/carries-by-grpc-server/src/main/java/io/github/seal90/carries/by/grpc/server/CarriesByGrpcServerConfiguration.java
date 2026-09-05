package io.github.seal90.carries.by.grpc.server;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class CarriesByGrpcServerConfiguration {

  public static final Context.Key<Metadata> SERVER_REQUEST_HEADER_KEY = Context.key("grpc.server.request.metadata");
  public static final Context.Key<Metadata> SERVER_RESPONSE_HEADER_KEY = Context.key("grpc.server.response.metadata");

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
        String transactionId = metadata.get(Metadata.Key.of("servicefilter-transaction-id", Metadata.ASCII_STRING_MARSHALLER));
        log.info("--- server receive client header servicefilter-transaction-id : {}", transactionId);

        Metadata responseMetadata = new Metadata();
        Context context = Context.current().withValue(SERVER_REQUEST_HEADER_KEY, metadata).withValue(SERVER_RESPONSE_HEADER_KEY, responseMetadata);
        ServerCall<ReqT, RespT> newServerCall = new ForwardingServerCall.SimpleForwardingServerCall<>(
            serverCall) {

          @Override
          public void sendHeaders(Metadata headers) {
            // If necessary, values can be passed to the client.
            headers.put(Metadata.Key.of("SERVER_TO_CLIENT_HEADER_KEY", Metadata.ASCII_STRING_MARSHALLER),
                "SERVER_TO_CLIENT_HEADER_VALUE");
            Metadata metadata = SERVER_RESPONSE_HEADER_KEY.get();
            headers.merge(metadata);
            super.sendHeaders(headers);
          }
        };
        return Contexts.interceptCall(context, newServerCall, metadata, serverCallHandler);
      }
    };
  }
}
