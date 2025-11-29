package io.github.seal90.http_server;

import io.github.seal90.http_facade.HelloReply;
import io.github.seal90.http_facade.HelloRequest;
import io.github.seal90.http_facade.HelloWorldFacade;
import io.github.seal90.serviceclient.ProtocolType;
import io.github.seal90.serviceclient.ServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RestController
public class HelloWorldFacadeImpl implements HelloWorldFacade {

  @ServiceClient(protocol = ProtocolType.HTTP, serviceName = "httpServer", channelName = "static://http://127.0.0.1:8080")
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
