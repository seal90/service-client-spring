package io.github.seal90.rsocket_server;

import io.github.seal90.rsocket_facade.HelloRSocketClient2ServerFacade;
import io.github.seal90.rsocket_facade.HelloRSocketServer2ClientFacade;
import io.github.seal90.rsocket_facade.HelloReply;
import io.github.seal90.rsocket_facade.HelloRequest;
import io.github.seal90.serviceclient.core.ProtocolType;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.github.seal90.serviceclient.rsocket.ChannelNameRSocketPrefix;
import io.github.seal90.serviceclient.rsocket.protocoltypefactory.client.RSocketClientRegistryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
public class HelloRSocketClient2ServerFacadeImpl implements HelloRSocketClient2ServerFacade {

  @ServiceClient(protocol = ProtocolType.RSOCKET, serviceName = "rsocket-client", channelName = ChannelNameRSocketPrefix.REGISTRATION_PREFIX)
  private HelloRSocketServer2ClientFacade server2ClientFacade;

  @Autowired
  private RSocketClientRegistryContext rSocketClientRegistryContext;

  @Override
  public Mono<Void> fireAndForget(HelloRequest request) {
    log.info("server fireAndForget: {}", request);
    return server2ClientFacade.fireAndForget(request);
  }

  @Override
  public Mono<HelloReply> requestResponse(HelloRequest request) {
    // You can obtain the client's RSocketRequester this way;
    // note that concurrency is not considered below.

//    ConcurrentHashMap<String, Map<String, ClientContext>> registry = rSocketClientRegistryContext.getRegistry();
//    Map<String, ClientContext> instances = registry.get("rsocket-client");
//    List<ClientContext> list = new ArrayList<>(instances.values());
//    ClientContext clientContext = list.getFirst();
//    RSocketRequester requester = clientContext.getRequester();
//    return requester.route("server2Client.requestResponse").data(request).retrieveMono(HelloReply.class);

    log.info("server requestResponse: {}", request);
    return server2ClientFacade.requestResponse(request);
  }

  @Override
  public Flux<HelloReply> requestStream(HelloRequest request) {
    log.info("server requestStream: {}", request);
    return server2ClientFacade.requestStream(request);
  }

  @Override
  public Flux<HelloReply> requestChannel(Flux<HelloRequest> requestFlux) {
    Flux<HelloRequest> requestShare = requestFlux.share();
    requestShare.subscribe(request -> {
      log.info("server requestChannel: {}", request);
    });
    return server2ClientFacade.requestChannel(requestShare);
  }
}
