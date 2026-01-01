package io.github.seal90.rsocket_client;

import io.github.seal90.rsocket_facade.HelloRSocketServer2ClientFacade;
import io.github.seal90.rsocket_facade.HelloReply;
import io.github.seal90.rsocket_facade.HelloRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * This class is used by the server to make reverse calls through registered client connections.
 *
 * Since this implementation is intended exclusively for a specific server and is not exposed
 * as an RSocket endpoint via Spring's component scanning, the @Controller annotation is not required.
 */
@Slf4j
@Component
public class HelloRSocketServer2ClientFacadeImpl implements HelloRSocketServer2ClientFacade {

  @Override
  public Mono<Void> fireAndForget(HelloRequest request) {
    log.info("client receive fireAndForget: {}", request);
    return Mono.empty();
  }

  @Override
  public Mono<HelloReply> requestResponse(HelloRequest request) {
    log.info("client receive requestResponse: {}", request);
    HelloReply reply = new HelloReply();
    reply.setMessage("client replay requestResponse " + request.getName());
    return Mono.just(reply);
  }

  @Override
  public Flux<HelloReply> requestStream(HelloRequest request) {
    log.info("client receive requestStream: {}", request);
    return Flux.range(1,10).map(index -> {
      HelloReply reply = new HelloReply();
      reply.setMessage("client replay requestStream" + index + " " + request.getName());
      return reply;
    });
  }

  @Override
  public Flux<HelloReply> requestChannel(Flux<HelloRequest> requestFlux) {
    requestFlux.subscribe(request -> {
      log.info("client receive requestChannel: {}", request);
    });
    return Flux.range(1,10).map(index -> {
      HelloReply reply = new HelloReply();
      reply.setMessage("client replay requestChannel " + index);
      return reply;
    });
  }
}
