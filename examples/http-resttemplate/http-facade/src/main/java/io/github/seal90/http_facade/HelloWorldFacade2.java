package io.github.seal90.http_facade;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface HelloWorldFacade2 {

  @GetMapping("/sayHello")
  HelloReply sayHello(@RequestBody HelloRequest request);

  @PostMapping("/mockSayHelloToOther")
  HelloReply mockSayHelloToOther(@RequestBody HelloRequest request);

}
