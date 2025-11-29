package io.github.seal90.http_facade;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

//@HttpExchange("/prefix")
public interface HelloWorldFacade {

  @GetExchange("/sayHello")
  HelloReply sayHello(@RequestBody HelloRequest request);

  @GetExchange("/mockSayHelloToOther")
  HelloReply mockSayHelloToOther(@RequestBody HelloRequest request);

}
