package io.github.seal90.rsocket_facade;

import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.service.RSocketExchange;
import reactor.core.publisher.Mono;

@RSocketExchange("prefix")
public interface HelloWorldFacade {

  @RSocketExchange("sayHello")
  Mono<HelloReply> sayHello(@Payload HelloRequest request);

  @RSocketExchange("mockSayHelloToOther")
  Mono<HelloReply> mockSayHelloToOther(@Payload HelloRequest request);

}
