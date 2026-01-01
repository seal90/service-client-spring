package io.github.seal90.rsocket_facade;

import org.springframework.messaging.rsocket.service.RSocketExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RSocketExchange("server2Client")
public interface HelloRSocketServer2ClientFacade {

  @RSocketExchange("fireAndForget")
  Mono<Void> fireAndForget(HelloRequest request);

  @RSocketExchange("requestResponse")
  Mono<HelloReply> requestResponse(HelloRequest request);

  @RSocketExchange("requestStream")
  Flux<HelloReply> requestStream(HelloRequest request);

  @RSocketExchange("requestChannel")
  Flux<HelloReply> requestChannel(Flux<HelloRequest> requestFlux);

}
