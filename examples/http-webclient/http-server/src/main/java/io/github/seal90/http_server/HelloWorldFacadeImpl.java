package io.github.seal90.http_server;

import io.github.seal90.http_facade.HelloReply;
import io.github.seal90.http_facade.HelloRequest;
import io.github.seal90.http_facade.HelloWorldFacade;
import io.github.seal90.serviceclient.ProtocolType;
import io.github.seal90.serviceclient.ServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
public class HelloWorldFacadeImpl implements HelloWorldFacade {

  @ServiceClient(protocol = ProtocolType.HTTP, serviceName = "httpServer", channelName = "static://http://127.0.0.1:8080")
  private HelloWorldFacade helloWorldFacade;

  @Override
  public Mono<HelloReply> sayHello(HelloRequest req) {
    log.info("sayHello {}", req.getName());

    Mono<HelloReply> reply = helloWorldFacade.mockSayHelloToOther(req);

    return reply.flatMap(mockReply -> {
      HelloReply helloReply = new HelloReply();
      helloReply.setMessage("sayHello ==> " + mockReply.getMessage());
      return Mono.just(helloReply);
    });

  }

  @Override
  public Mono<HelloReply> mockSayHelloToOther(HelloRequest req) {
    log.info("mockSayHelloToOther {}", req.getName());

    HelloReply helloReply = new HelloReply();
    helloReply.setMessage("mockSayHelloToOther ==> " + req.getName());
    return Mono.just(helloReply);
  }

}
