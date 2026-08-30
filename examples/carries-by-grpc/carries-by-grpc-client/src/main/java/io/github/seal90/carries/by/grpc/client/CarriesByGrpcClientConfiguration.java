package io.github.seal90.carries.by.grpc.client;

import io.github.seal90.serviceclient.carries.by.grpc.protocoltypefactory.ProtocolTypeCarriesByGrpcFactory;
import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.github.seal90.serviceclient.grpc.protocoltypefactory.ProtocolTypeGrpcFactory.CHANNEL_NAME_KEY;
import static io.github.seal90.serviceclient.grpc.protocoltypefactory.ProtocolTypeGrpcFactory.SERVICE_NAME_KEY;


@Slf4j
@Configuration(proxyBeanMethods = false)
public class CarriesByGrpcClientConfiguration {

  @Bean
  @ServiceClientInterceptor
  public ClientInterceptor clientInterceptor() {
    return new ClientInterceptor() {
      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                                 CallOptions callOptions, Channel next) {

        String serviceName = callOptions.getOption(SERVICE_NAME_KEY);
        String channelName = callOptions.getOption(CHANNEL_NAME_KEY);
        log.info("--- client interceptor serverName: {}, channelName: {}", serviceName, channelName);

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
            next.newCall(method, callOptions)) {
          @Override
          public void start(Listener responseListener, Metadata headers) {

            headers.put(Metadata.Key.of("CLIENT_TO_SERVER_HEADER_KEY", Metadata.ASCII_STRING_MARSHALLER),
                "CLIENT_TO_SERVER_HEADER_VALUE");

            headers.put(Metadata.Key.of("overlay-ns", Metadata.ASCII_STRING_MARSHALLER),
                "test");

            super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                responseListener) {
              @Override
              public void onHeaders(Metadata headers) {
                String value = headers.get(Metadata.Key.of("SERVER_TO_CLIENT_HEADER_KEY",
                    Metadata.ASCII_STRING_MARSHALLER));
                log.info("--- client receive sever header SERVER_TO_CLIENT_HEADER_KEY : {}", value);
                super.onHeaders(headers);
              }
            }, headers);
          }
        };
      }
    };
  }

  @Bean
  @ServiceClientInterceptor
  public ClientInterceptor servicefilterAdapter(){
    return new ClientInterceptor() {
      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> method,
                                                                 final CallOptions callOptions, final Channel next) {

        String stashServiceName = callOptions.getOption(SERVICE_NAME_KEY);
        if(stashServiceName == null) {
          stashServiceName = callOptions.getOption(ProtocolTypeCarriesByGrpcFactory.SERVICE_NAME_KEY);
        }
        String serviceName = stashServiceName;

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
          @Override
          public void start(Listener<RespT> responseListener, Metadata headers) {
            if(serviceName != null) {
              headers.put(Metadata.Key.of("servicefilter-app-id", Metadata.ASCII_STRING_MARSHALLER), serviceName);
            }
            super.start(responseListener, headers);
          }
        };
      }
    };
  }
}
