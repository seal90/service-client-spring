package io.github.seal90.http_facade;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;

@HttpExchange("/prefix")
public interface HelloWorldFacade {

  @GetExchange("/sayHello")
  Mono<HelloReply> sayHello(@RequestBody HelloRequest request);

  @GetExchange("/mockSayHelloToOther")
  Mono<HelloReply> mockSayHelloToOther(@RequestBody HelloRequest request);

}
