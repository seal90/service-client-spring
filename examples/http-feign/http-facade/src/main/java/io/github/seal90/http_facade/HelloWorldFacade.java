package io.github.seal90.http_facade;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface HelloWorldFacade {

  // Feign's @GetMapping annotation does not support the use of @RequestBody,
  // as HTTP GET requests, by specification, should not include a request body.
  @PostMapping("/sayHello")
  HelloReply sayHello(@RequestBody HelloRequest request);

  @PostMapping("/mockSayHelloToOther")
  HelloReply mockSayHelloToOther(@RequestBody HelloRequest request);

}
