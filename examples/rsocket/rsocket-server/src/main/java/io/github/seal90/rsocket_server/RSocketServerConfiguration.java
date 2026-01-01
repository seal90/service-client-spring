package io.github.seal90.rsocket_server;

import io.netty.buffer.ByteBuf;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.util.RSocketProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class RSocketServerConfiguration {

  @Autowired
  private RSocketStrategies rSocketStrategies;

  @Bean
  public RSocketServerCustomizer rSocketServerCustomizer() {
    return server -> server
        .interceptors(interceptorRegistry -> {
          interceptorRegistry.forRequester(rSocketInterceptors -> {
//            rSocketInterceptors.add(new MetadataDealRSocketInterceptor(rSocketStrategies));
          });
          interceptorRegistry.forResponder(rSocketInterceptors -> {
            rSocketInterceptors.add(new MetadataDealRSocketInterceptor(rSocketStrategies.metadataExtractor()));
          });
        })
        .payloadDecoder(PayloadDecoder.ZERO_COPY);
  }

  public static class MetadataDealRSocketInterceptor implements RSocketInterceptor {

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
      return super.fireAndForget(payload);
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      System.out.println("-------------------------------------------------------------");
      Map<String, Object> val = metadataExtractor.extract(payload, MimeTypeUtils.ALL);
      val.forEach((key, value) -> System.out.println(key + ":" + value));

      ByteBuf originalMetadataByteBuf = payload.sliceMetadata();
      System.out.println("-------------------------------------------------------------");
      CompositeMetadata originalCompositeMetadata = new CompositeMetadata(originalMetadataByteBuf, false);
      for (CompositeMetadata.Entry entry : originalCompositeMetadata) {
        System.out.println(entry.getMimeType() + ":" + entry.getContent());
      }
      System.out.println("-------------------------------------------------------------");
      return super.requestResponse(payload);
    }
  }

}
