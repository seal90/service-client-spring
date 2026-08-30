package io.github.seal90.carries.by.rsocket.facade;

import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Handling Metadata with Context
public interface HelloWorldFacade {

    @MessageMapping("rsocket.msg.sayHello")
    default HelloReply sayHello(HelloRequest request) {
        return null;
    }

    @MessageMapping("rsocket.msg.requestStream.sayHello")
    default Flux<HelloReply> sayHello(Mono<HelloRequest> request) {
        return null;
    }
}
