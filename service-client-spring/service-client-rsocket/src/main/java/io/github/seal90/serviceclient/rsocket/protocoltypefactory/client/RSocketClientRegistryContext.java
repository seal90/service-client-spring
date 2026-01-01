package io.github.seal90.serviceclient.rsocket.protocoltypefactory.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.metadata.CompositeMetadata;
import lombok.Getter;
import org.reactivestreams.Publisher;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class RSocketClientRegistryContext {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final ConcurrentHashMap<String, Map<String, ClientContext>> registry = new ConcurrentHashMap<>();

  public Tuple2<String, String> registry(String data, RSocketRequester requester) {
    try {
      ClientRegistration registration = objectMapper.readValue(data, ClientRegistration.class);
      String serviceId = registration.getServiceId();
      String instanceId = registration.getInstanceId();
      ClientContext context = new ClientContext(serviceId, instanceId, requester, registration.getTags());

      registry.computeIfAbsent(serviceId, k -> new ConcurrentHashMap<>())
          .put(instanceId, context);

      return Tuples.of(serviceId, instanceId);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public RSocketRequester buildByServiceId(String serviceId, RSocketRequester.Builder builder) {
    RSocketClient rSocketClient = new ClientRegistrationRSocketClient(serviceId, registry);
    // Here, the Spring source code has been modified to support transports via RSocketClient.
    return builder.transports(rSocketClient);
  }

  public void remove(Tuple2<String, String> key) {
    String serviceId = key.getT1();
    String instanceId = key.getT2();
    Map<String, ClientContext> instances = registry.get(serviceId);
    if (instances != null) {
      instances.remove(instanceId);
      if (instances.isEmpty()) {
        registry.remove(serviceId);
      }
    }
  }

  // TODO Extract the load-balancing logic.
  private static class ClientRegistrationRSocketClient implements RSocketClient {

    private final String serviceId;
    private final ConcurrentHashMap<String, Map<String, ClientContext>> registry;
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public ClientRegistrationRSocketClient(String serviceId
        , ConcurrentHashMap<String, Map<String, ClientContext>> registry) {
      this.serviceId = serviceId;
      this.registry = registry;
    }

    @Override
    public Mono<RSocket> source() {
      return Mono.justOrEmpty(selectHealthyRSocket());
    }

    @Override
    public Mono<Void> fireAndForget(Mono<Payload> payloadMono) {
      return payloadMono.flatMap(payload -> {
        RSocket rsocket = selectHealthyRSocket();
        if (rsocket == null || rsocket.isDisposed()) {
          return Mono.error(new IllegalStateException("No healthy instance for fire-and-forget"));
        }
        return rsocket.fireAndForget(payload);
      });
    }

    @Override
    public Mono<Payload> requestResponse(Mono<Payload> payloadMono) {
      return payloadMono.flatMap(payload -> {
        RSocket rsocket = selectHealthyRSocket();
        if (rsocket == null || rsocket.isDisposed()) {
          return Mono.error(new IllegalStateException("No healthy instance available for service: " + serviceId));
        }
        return rsocket.requestResponse(payload);
      });
    }

    @Override
    public Flux<Payload> requestStream(Mono<Payload> payloadMono) {
      return payloadMono.flatMapMany(payload -> {
        RSocket rsocket = selectHealthyRSocket();
        if (rsocket == null || rsocket.isDisposed()) {
          return Flux.error(new IllegalStateException("No healthy instance for request-stream"));
        }
        return rsocket.requestStream(payload);
      });
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      return Mono.fromSupplier(this::selectHealthyRSocket).flatMapMany(rSocket -> {
        if (rSocket == null || rSocket.isDisposed()) {
          return Flux.error(new IllegalStateException("No healthy instance for request-stream"));
        }
        return rSocket.requestChannel(payloads);
      });
    }

    @Override
    public Mono<Void> metadataPush(Mono<Payload> payloadMono) {
      return payloadMono.flatMap(payload -> {
        RSocket rsocket = selectHealthyRSocket();
        if (rsocket == null || rsocket.isDisposed()) {
          return Mono.error(new IllegalStateException("No healthy instance for metadata-push"));
        }
        return rsocket.metadataPush(payload);
      });
    }

    @Override
    public void dispose() {
    }

    private RSocket selectHealthyRSocket() {
      Map<String, ClientContext> instances = registry.get(serviceId);
      if (instances == null || instances.isEmpty()) {
        return null;
      }

      List<ClientContext> snapshot = new ArrayList<>(instances.values());
      if (snapshot.isEmpty()) {
        return null;
      }

      int size = snapshot.size();
      int startIndex = Math.abs(roundRobinIndex.getAndIncrement() % size);

      for (int i = 0; i < size; i++) {
        int index = (startIndex + i) % size;
        ClientContext ctx = snapshot.get(index);
        RSocket rsocket = ctx.getRequester().rsocket();

        if (rsocket != null && !rsocket.isDisposed()) {
          return rsocket;
        }
      }

      return null;
    }
  }

}
