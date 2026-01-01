package io.github.seal90.http_server;

import io.github.seal90.http_facade.HelloReply;
import io.github.seal90.http_facade.HelloRequest;
import io.github.seal90.http_facade.HelloWorldFacade;
import io.github.seal90.serviceclient.core.ProtocolType;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.github.seal90.serviceclient.http.feign.ProtocolTypeHttpFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HelloWorldFacadeImpl implements HelloWorldFacade {

  @ServiceClient(protocol = ProtocolTypeHttpFeign.HTTP_FEIGN, serviceName = "feignServer")
  private HelloWorldFacade helloWorldFacade;

  @Override
  public HelloReply sayHello(HelloRequest req) {
    log.info("sayHello {}", req.getName());

    HelloReply reply = helloWorldFacade.mockSayHelloToOther(req);
    HelloReply helloReply = new HelloReply();
    helloReply.setMessage("sayHello ==> " + reply.getMessage());
    return helloReply;
  }

  @Override
  public HelloReply mockSayHelloToOther(HelloRequest req) {
    log.info("mockSayHelloToOther {}", req.getName());

    HelloReply helloReply = new HelloReply();
    helloReply.setMessage("mockSayHelloToOther ==> " + req.getName());
    return helloReply;
  }

}
