package io.github.seal90.rsocket_server;

import io.github.seal90.rsocket_facade.HelloReply;
import io.github.seal90.rsocket_facade.HelloRequest;
import io.github.seal90.rsocket_facade.HelloWorldFacade;
import io.github.seal90.serviceclient.core.ProtocolType;
import io.github.seal90.serviceclient.core.ServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class HelloWorldFacadeImpl implements HelloWorldFacade {

  @ServiceClient(protocol = ProtocolType.RSOCKET, serviceName = "rsocket-server", channelName = "static://127.0.0.1:9898")
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
