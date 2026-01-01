package io.github.seal90.serviceclient.rsocket.protocoltypefactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seal90.serviceclient.core.ChannelNamePrefix;
import io.github.seal90.serviceclient.core.ProtocolType;
import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.github.seal90.serviceclient.core.util.ApplicationContextBeanLookupUtils;
import io.github.seal90.serviceclient.core.util.MD5Util;
import io.github.seal90.serviceclient.rsocket.ChannelNameRSocketPrefix;
import io.github.seal90.serviceclient.rsocket.ProtocolTypeRSocketProperties;
import io.github.seal90.serviceclient.rsocket.ProtocolTypeRSocketServiceProperties;
import io.github.seal90.serviceclient.rsocket.protocoltypefactory.client.ClientRegistration;
import io.github.seal90.serviceclient.rsocket.protocoltypefactory.client.RSocketClientRegistryContext;
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
import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.messaging.rsocket.service.RSocketServiceProxyFactory;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Member;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.NAME_RESOLVED_FLAG;

public class ProtocolTypeRSocketFactory implements ProtocolTypeFactory, ApplicationContextAware, EnvironmentAware {

  public static final String INSTANCE_REGISTRY_MIMETYPE = "text/plain.x.instance_registry.v0";
  public static final String SERVICE_NAME_MIMETYPE = "text/plain.x."+ SERVICE_NAME.toLowerCase(Locale.ROOT) +".v0";
  public static final String CHANNEL_NAME_MIMETYPE = "text/plain.x."+ CHANNEL_NAME.toLowerCase(Locale.ROOT) +".v0";
  public static final String NAME_RESOLVED_FLAG_MIMETYPE = "text/plain.x."+ NAME_RESOLVED_FLAG.toLowerCase(Locale.ROOT) +".v0";;
  private static final List<String> APPEND_MIMETYPE = List.of(SERVICE_NAME_MIMETYPE, CHANNEL_NAME_MIMETYPE, NAME_RESOLVED_FLAG_MIMETYPE);

  private final ConcurrentHashMap<String, RSocketServiceProxyFactory> cacheFactory = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private ApplicationContext applicationContext;
  private Environment environment;

  @Override
  public <T> T create(Member injectionTarget, Class<T> injectionType, ServiceClient annotation) {
    final String serviceName = annotation.serviceName();
    final String channelName = annotation.channelName();
    final String[] interceptors = annotation.interceptors();

    String cacheKeyContent = serviceName + ":" + channelName + ":" + String.join(":", interceptors);
    String cacheKey = MD5Util.md5Hash(cacheKeyContent);

    RSocketServiceProxyFactory factory = cacheFactory.computeIfAbsent(cacheKey, k -> {
      RSocketRequester rSocketRequester = buildRSocketRequester(serviceName, channelName, interceptors);
      return RSocketServiceProxyFactory.builder().rsocketRequester(rSocketRequester).build();
    });
    return factory.createClient(injectionType);
  }

  private RSocketRequester buildRSocketRequester(String serviceName, String channelName, String[] interceptors) {
    if(!channelName.isEmpty()) {
      if(ChannelNamePrefix.isContext(channelName)) {
        if (interceptors.length > 0) {
          throw new IllegalArgumentException("interceptors are not allowed for channel name 'context'");
        }

        return (RSocketRequester) applicationContext.getBean(ChannelNamePrefix.extractContext(channelName));
      } else if (ChannelNameRSocketPrefix.isRegistration(channelName)) {
        RSocketClientRegistryContext clientRegistryContext = applicationContext.getBean(RSocketClientRegistryContext.class);
        RSocketRequester.Builder builder = applicationContext.getBean(RSocketRequester.Builder.class);
        return clientRegistryContext.buildByServiceId(serviceName, builder);
      }
    }

    String address = findAddresses(serviceName, channelName);
    if(address == null) {
      throw new IllegalArgumentException("can't not parse address by serviceName and channelName");
    }

    return buildRSocketRequester(serviceName, channelName, interceptors, address);
  }

  private String findAddresses(String serviceName, String channelName) {
    ProtocolTypeRSocketProperties rSocketProperties = applicationContext.getBean(ProtocolTypeRSocketProperties.class);
    String address;
    if(!channelName.isEmpty()) {
      if (ChannelNamePrefix.isStatic(channelName)) {
        address = ChannelNamePrefix.extractStatic(channelName);
        address = environment.resolveRequiredPlaceholders(address);
      } else if (ChannelNamePrefix.isChannel(channelName)) {
        address = rSocketProperties.addressByChannel(ChannelNamePrefix.extractChannel(channelName));
      } else if (ChannelNamePrefix.isDefault(channelName)) {
        address = rSocketProperties.addressByDefault();
      } else if (ChannelNamePrefix.isLb(channelName)) {
        // Other parsing, such as registration center
        address = channelName;
      } else {
        // default is channel name
        address = rSocketProperties.addressByChannel(channelName);
      }
    } else {
      address = rSocketProperties.addressByServiceName(serviceName);
      if(address == null) {
        address = ChannelNamePrefix.withLb(serviceName);
      }
    }
    return address;
  }

  private RSocketRequester buildRSocketRequester(String serviceName, String channelName, String[] interceptors, String address) {

    List<RSocketInterceptor> rSocketInterceptors = new ArrayList<>();

    // TODO Abstract an interface similar to WebExchange to avoid repeated manipulation of metadata.
    // forRequester forResponder different interface
    for(String interceptor : interceptors) {
      RSocketInterceptor rSocketInterceptor = (RSocketInterceptor)applicationContext.getBean(interceptor);
      rSocketInterceptors.add(rSocketInterceptor);
    }
    List<RSocketInterceptor> globalInterceptors = ApplicationContextBeanLookupUtils
        .getBeansWithAnnotation(applicationContext, RSocketInterceptor.class, ServiceClientInterceptor.class);
    rSocketInterceptors.addAll(globalInterceptors);
    AnnotationAwareOrderComparator.sort(rSocketInterceptors);
    rSocketInterceptors.addFirst(new ServiceNameRemoveRSocketInterceptor());
    rSocketInterceptors.add(new ServiceNameAddRSocketInterceptor(serviceName, channelName));

    RSocketRequester.Builder builder = applicationContext.getBean(RSocketRequester.Builder.class);

    builder = builder.rsocketConnector(connector -> {
      connector.interceptors(interceptorRegistry -> {
        interceptorRegistry.forRequester(requester -> {
          // Items added later are executed first
          requester.addAll(rSocketInterceptors);
        });
      });
    }).rsocketStrategies(configurer ->
        configurer.metadataExtractorRegistry(registry -> {
          registry.metadataToExtract(MimeType.valueOf(SERVICE_NAME_MIMETYPE), String.class, SERVICE_NAME);
          registry.metadataToExtract(MimeType.valueOf(CHANNEL_NAME_MIMETYPE), String.class, CHANNEL_NAME);
          registry.metadataToExtract(MimeType.valueOf(NAME_RESOLVED_FLAG_MIMETYPE), String.class, NAME_RESOLVED_FLAG);
        })
    );

    // TODO Pass authentication information in metadata.
    String curServiceName = environment.getProperty("spring.application.name");
    ClientRegistration clientRegistration = new ClientRegistration(curServiceName, UUID.randomUUID().toString(), Collections.EMPTY_MAP);
    CompositeByteBuf setupMetadataBuf = ByteBufAllocator.DEFAULT.compositeBuffer();
    try {
      addMetadataEntry(setupMetadataBuf, INSTANCE_REGISTRY_MIMETYPE, objectMapper.writeValueAsString(clientRegistration));
      builder = builder.setupRoute("register").setupData(objectMapper.writeValueAsString(clientRegistration))
          .setupMetadata(setupMetadataBuf, MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.toString()));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    ProtocolTypeRSocketProperties properties = applicationContext.getBean(ProtocolTypeRSocketProperties.class);
    ProtocolTypeRSocketServiceProperties serviceProperties = properties.getServices().get(serviceName);
    if(serviceProperties != null  && serviceProperties.getRSocketMessageHandlerBeanNames() != null
        && serviceProperties.getRSocketMessageHandlerBeanNames().length > 0) {
      String[] rSocketMessageHandlerBeanNames = serviceProperties.getRSocketMessageHandlerBeanNames();
      List<Object> beans = Arrays.stream(rSocketMessageHandlerBeanNames).map(
          beanNames -> applicationContext.getBean(beanNames)).toList();
      RSocketStrategies rSocketStrategies = applicationContext.getBean(RSocketStrategies.class);
      SocketAcceptor responder = RSocketMessageHandler.responder(rSocketStrategies, beans.toArray());
      builder = builder.rsocketConnector(connector -> connector.acceptor(responder));
    }

    // TODO Supports selecting the target RSocket based on characteristic requests.
    String[] addr = address.split(":");
    return builder.tcp(addr[0], Integer.parseInt(addr[1]));
//    List<LoadbalanceTarget> targets = addresses.stream().map(
//        address -> LoadbalanceTarget.from(address.getHost()+address.getPort()
//            , TcpClientTransport.create(address.getHost(), address.getPort()))
//    ).toList();
//    Flux<List<LoadbalanceTarget>> serverInstances = Flux.just(targets);
//    return builder.transports(serverInstances, new RoundRobinLoadbalanceStrategy());
  }

  private void addMetadataEntry(CompositeByteBuf composite, String mimeType, String value) {
    ByteBuf buf = ByteBufAllocator.DEFAULT.buffer().writeBytes(value.getBytes(StandardCharsets.UTF_8));
    CompositeMetadataCodec.encodeAndAddMetadata(composite, ByteBufAllocator.DEFAULT, mimeType, buf);
  }

  @Override
  public String supportProtocol() {
    return ProtocolType.RSOCKET;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  private static class ServiceNameAddRSocketInterceptor implements RSocketInterceptor {

    private final String serviceName;
    private final String channelName;

    public ServiceNameAddRSocketInterceptor(String serviceName, String channelName) {
      this.serviceName = serviceName;
      this.channelName = channelName;
    }

    @Override
    public RSocket apply(RSocket rSocket) {
      return new ServiceNameAddRSocket(rSocket, serviceName, channelName);
    }
  }

  private static class ServiceNameAddRSocket extends RSocketProxy {

    private final String serviceName;
    private final String channelName;

    public ServiceNameAddRSocket(RSocket source, String serviceName, String channelName) {
      super(source);
      this.serviceName = serviceName;
      this.channelName = channelName;
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
      return withAddedMetadata(payload, super::fireAndForget);
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      return withAddedMetadata(payload, super::requestResponse);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
      return withAddedMetadata(payload, super::requestStream);
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
      return withAddedMetadata(payload, super::metadataPush);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      return super.requestChannel(Flux.from(payloads)
          .switchOnFirst((signal, payloadFlux) -> {
            if (!signal.hasValue()) {
              return payloadFlux;
            }

            Payload original = signal.get();
            Payload enhanced;
            try {
              enhanced = buildServiceNamePayload(original);
            } catch (Throwable t) {
              return Flux.error(t);
            }

            return Flux.concat(
                Mono.just(enhanced),
                payloadFlux.skip(1)
            );
          }));
    }

    private <T> T withAddedMetadata(Payload payload, Function<Payload, T> delegate) {
      Payload newPayload = buildServiceNamePayload(payload);

      T result = delegate.apply(newPayload);
      if (result instanceof Mono<?>) {
        @SuppressWarnings("unchecked")
        Mono<?> mono = (Mono<?>) result;
        return (T) mono.doOnTerminate(newPayload::release);
      } else if (result instanceof Flux<?>) {
        @SuppressWarnings("unchecked")
        Flux<?> flux = (Flux<?>) result;
        return (T) flux.doOnTerminate(newPayload::release);
      } else {
        return result;
      }
    }

    private Payload buildServiceNamePayload(Payload payload) {
      ByteBuf newData = payload.sliceData().retain();

      CompositeByteBuf newCompositeMetadata = ByteBufAllocator.DEFAULT.compositeBuffer();

      if (payload.hasMetadata()) {
        CompositeMetadata original = new CompositeMetadata(payload.sliceMetadata(), false);
        for (CompositeMetadata.Entry entry : original) {
          CompositeMetadataCodec.encodeAndAddMetadata(
              newCompositeMetadata,
              ByteBufAllocator.DEFAULT,
              entry.getMimeType(),
              entry.getContent().retain()
          );
        }
      }

      addMetadataEntry(newCompositeMetadata, SERVICE_NAME_MIMETYPE, serviceName);
      addMetadataEntry(newCompositeMetadata, CHANNEL_NAME_MIMETYPE, channelName);
      addMetadataEntry(newCompositeMetadata, NAME_RESOLVED_FLAG_MIMETYPE, "true");

      return DefaultPayload.create(newData, newCompositeMetadata);
    }

    private void addMetadataEntry(CompositeByteBuf composite, String mimeType, String value) {
      ByteBuf buf = ByteBufAllocator.DEFAULT.buffer().writeBytes(value.getBytes(StandardCharsets.UTF_8));
      CompositeMetadataCodec.encodeAndAddMetadata(composite, ByteBufAllocator.DEFAULT, mimeType, buf);
    }
  }

  private static class ServiceNameRemoveRSocketInterceptor implements RSocketInterceptor {

    @Override
    public RSocket apply(RSocket rSocket) {
      return new ServiceNameRemoveRSocket(rSocket);
    }
  }

  private static class ServiceNameRemoveRSocket extends RSocketProxy {

    public ServiceNameRemoveRSocket(RSocket source) {
      super(source);
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
      return withAddedMetadata(payload, super::fireAndForget);
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      return withAddedMetadata(payload, super::requestResponse);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
      return withAddedMetadata(payload, super::requestStream);
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
      return withAddedMetadata(payload, super::metadataPush);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      return super.requestChannel(Flux.from(payloads)
          .switchOnFirst((signal, payloadFlux) -> {
            if (!signal.hasValue()) {
              return payloadFlux;
            }

            Payload original = signal.get();
            Payload enhanced;
            try {
              enhanced = buildServiceNamePayload(original);
            } catch (Throwable t) {
              return Flux.error(t);
            }

            return Flux.concat(
                Mono.just(enhanced),
                payloadFlux.skip(1)
            );
          }));
    }

    private <T> T withAddedMetadata(Payload payload, Function<Payload, T> delegate) {
      Payload newPayload = buildServiceNamePayload(payload);

      T result = delegate.apply(newPayload);
      if (result instanceof Mono<?>) {
        @SuppressWarnings("unchecked")
        Mono<?> mono = (Mono<?>) result;
        return (T) mono.doOnTerminate(newPayload::release);
      } else if (result instanceof Flux<?>) {
        @SuppressWarnings("unchecked")
        Flux<?> flux = (Flux<?>) result;
        return (T) flux.doOnTerminate(newPayload::release);
      } else {
        return result;
      }
    }

    private Payload buildServiceNamePayload(Payload payload) {
      ByteBuf newData = payload.sliceData().retain();

      CompositeByteBuf newCompositeMetadata = ByteBufAllocator.DEFAULT.compositeBuffer();

      if (payload.hasMetadata()) {
        CompositeMetadata original = new CompositeMetadata(payload.sliceMetadata(), false);
        for (CompositeMetadata.Entry entry : original) {
          if(!APPEND_MIMETYPE.contains(entry.getMimeType())) {
            CompositeMetadataCodec.encodeAndAddMetadata(
                newCompositeMetadata,
                ByteBufAllocator.DEFAULT,
                entry.getMimeType(),
                entry.getContent().retain()
            );
          }
        }
      }

      return DefaultPayload.create(newData, newCompositeMetadata);
    }
  }
}