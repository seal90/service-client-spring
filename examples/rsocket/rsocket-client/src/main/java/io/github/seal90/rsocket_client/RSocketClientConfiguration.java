package io.github.seal90.rsocket_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.github.seal90.serviceclient.rsocket.protocoltypefactory.client.ClientRegistration;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.CompositeMetadataCodec;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.util.DefaultPayload;
import io.rsocket.util.RSocketProxy;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.MimeType;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

@Slf4j
@Configuration
public class RSocketClientConfiguration {

  public static String CLIENT_TO_SERVER_HEADER_VALUE_MIMETYPE = "text/plain.x.seal.client_to_server_header_value.v0";
  public static String OVERLAY_NS_MIMETYPE = "text/plain.x.seal.overlay_ns.v0";
  public static String SERVER_TO_CLIENT_HEADER_KEY_MIMETYPE = "text/plain.x.server_to_client_header_key.v0";

  @Autowired
  private RSocketStrategies rSocketStrategies;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Bean
  public RSocketRequester rSocketRequester(RSocketRequester.Builder rsocketRequesterBuilder,
                                           RSocketStrategies rsocketStrategies, HelloRSocketServer2ClientFacadeImpl server2ClientFacade) throws JsonProcessingException {

    SocketAcceptor responder = RSocketMessageHandler.responder(rsocketStrategies, server2ClientFacade);
    ClientRegistration clientRegistration = new ClientRegistration("rsocket-client", UUID.randomUUID().toString(), Collections.EMPTY_MAP);
    String data = objectMapper.writeValueAsString(clientRegistration);
    log.info("register to server");
    RSocketRequester rsocketRequester = rsocketRequesterBuilder
        .setupRoute("register")
        .setupData(data)
        .rsocketConnector(connector -> connector.acceptor(responder))
        .tcp("127.0.0.1", 9898);

    // The above does not connect immediately. When requests are made, a shared connection is established transparently and used.
    rsocketRequester.rsocketClient().source().block();

    rsocketRequester.rsocketClient()
        .source().flatMap(RSocket::onClose).repeat().retryWhen(Retry.indefinitely())
        .doOnError(error -> log.warn("Connection CLOSED"))
        .doFinally(consumer -> log.info("Client DISCONNECTED"))
        .subscribe();

    return rsocketRequester;
  }

  @Bean
  @ServiceClientInterceptor
  public RSocketInterceptor metadataDealRSocketInterceptor() {
    return new MetadataDealRSocketInterceptor(rSocketStrategies.metadataExtractor());
  }

  private static class MetadataDealRSocketInterceptor implements RSocketInterceptor {

    private final MetadataExtractor metadataExtractor;

    public MetadataDealRSocketInterceptor(MetadataExtractor metadataExtractor) {
      this.metadataExtractor = metadataExtractor;
    }

    @Override
    public RSocket apply(RSocket rSocket) {
      return new MetadataDealRSocket(rSocket, metadataExtractor);
    }
  }

  private static class MetadataDealRSocket extends RSocketProxy {

    private final MetadataExtractor metadataExtractor;

    public MetadataDealRSocket(RSocket source, MetadataExtractor metadataExtractor) {
      super(source);
      this.metadataExtractor = metadataExtractor;
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
      Payload enhanced = enhanceRequestPayload(payload);
      logReqPayload(enhanced);
      return super.fireAndForget(enhanced)
          .doOnTerminate(enhanced::release);
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
      Payload enhanced = enhanceRequestPayload(payload);
      logReqPayload(enhanced);
      return super.metadataPush(enhanced)
          .doOnTerminate(enhanced::release);
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      Payload enhanced = enhanceRequestPayload(payload);
      logReqPayload(enhanced);
      return super.requestResponse(enhanced)
          .doOnNext(this::logRespPayload)
          .doOnTerminate(enhanced::release);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
      Payload enhanced = enhanceRequestPayload(payload);
      logReqPayload(enhanced);
      return super.requestStream(enhanced)
          .doOnTerminate(enhanced::release)
          .switchOnFirst((signal, payloadFlux) -> {
            if (signal.hasValue()) {
              logRespPayload(signal.get());
            }
            return payloadFlux;
          });
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      Flux<Payload> enhancedPayloads = Flux.from(payloads)
          .switchOnFirst((signal, payloadFlux) -> {
            if (!signal.hasValue()) {
              return payloadFlux;
            }
            Payload original = signal.get();
            Payload enhanced = enhanceRequestPayload(original);
            logReqPayload(enhanced);
            return Flux.concat(
                Mono.just(enhanced),
                payloadFlux.skip(1)
            );
          });

      return super.requestChannel(enhancedPayloads)
          .switchOnFirst((signal, payloadFlux) -> {
            if (signal.hasValue()) {
              logRespPayload(signal.get());
            }
            return payloadFlux;
          });
    }


    private Payload enhanceRequestPayload(Payload original) {
      original.retain();
      try {
        ByteBuf newData = original.data().retain();
        ByteBuf newMetadata = buildNewCompositeMetadata(original.hasMetadata() ? original.metadata() : null);
        return DefaultPayload.create(newData, newMetadata);
      } catch (Throwable t) {
        original.release();
        throw Exceptions.propagate(t);
      }
    }

    private ByteBuf buildNewCompositeMetadata(ByteBuf originalMetadataByteBuf) {
      CompositeByteBuf newCompositeMetadata = ByteBufAllocator.DEFAULT.compositeBuffer();

      if (originalMetadataByteBuf != null && originalMetadataByteBuf.isReadable()) {
        CompositeMetadata originalCompositeMetadata = new CompositeMetadata(originalMetadataByteBuf, false);
        for (CompositeMetadata.Entry entry : originalCompositeMetadata) {
          CompositeMetadataCodec.encodeAndAddMetadata(
              newCompositeMetadata,
              ByteBufAllocator.DEFAULT,
              entry.getMimeType(),
              entry.getContent().retain()
          );
        }
      }

      // Add custom metadata
      addMetadataEntry(newCompositeMetadata, CLIENT_TO_SERVER_HEADER_VALUE_MIMETYPE, "CLIENT_TO_SERVER_HEADER_VALUE");
      addMetadataEntry(newCompositeMetadata, OVERLAY_NS_MIMETYPE, "test");

      return newCompositeMetadata;
    }

    private void addMetadataEntry(CompositeByteBuf composite, String mimeType, String value) {
      ByteBuf buf = ByteBufAllocator.DEFAULT.buffer()
          .writeBytes(value.getBytes(StandardCharsets.UTF_8));
      CompositeMetadataCodec.encodeAndAddMetadata(composite, ByteBufAllocator.DEFAULT, mimeType, buf);
    }

    private void logReqPayload(Payload payload) {
      try {
        Map<String, Object> metadata = metadataExtractor.extract(payload, MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.toString()));
        Object serviceName = metadata.get(SERVICE_NAME);
        Object channelName = metadata.get(CHANNEL_NAME);
        log.info("--- client interceptor serverName: {}, channelName: {}", serviceName, channelName);
      } catch (Exception e) {
        log.warn("Failed to log request payload", e);
      }
    }

    private void logRespPayload(Payload payload) {
      try {
        log.info("--logRespPayload-----------------------------------------------------------");
        for(CompositeMetadata.Entry entry : new CompositeMetadata(payload.metadata(), false)) {
          log.info("--- client receive sever metadata {} : {}", entry.getMimeType(), entry.getContent());
        }
        log.info("--logRespPayload-----------------------------------------------------------");

        Map<String, Object> metadata = metadataExtractor.extract(payload, MediaType.valueOf(SERVER_TO_CLIENT_HEADER_KEY_MIMETYPE));
        Object value = metadata.get("server_to_client_header_key");
        log.info("--- client receive sever header SERVER_TO_CLIENT_HEADER_KEY : {}", value);
      } catch (Exception e) {
        log.warn("Failed to log response payload", e);
      }
    }
  }

}
