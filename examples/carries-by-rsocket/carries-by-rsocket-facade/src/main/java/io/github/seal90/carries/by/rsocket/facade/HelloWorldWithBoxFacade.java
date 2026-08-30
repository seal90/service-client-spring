package io.github.seal90.carries.by.rsocket.facade;

import io.github.seal90.serviceclient.carries.by.rsocket.api.Request;
import io.github.seal90.serviceclient.carries.by.rsocket.api.Response;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Use custom request/response types to handle both metadata and payload
// Define business request/response messages using Protobuf and transmit the data in the body
public interface HelloWorldWithBoxFacade {

    @MessageMapping("rsocket.msg.box.sayHello")
    default Response<HelloReply> sayHello(Request<HelloRequest> request) {
        return null;
    }

    @MessageMapping("rsocket.msg.box.sayHelloFlux")
    default Flux<Response<HelloReply>> sayHelloFlux(Mono<Request<HelloRequest>> request) {
        return null;
    }

    @MessageMapping("rsocket.msg.box.sayHelloAllFlux")
    default Flux<Response<HelloReply>> sayHelloAllFlux(Flux<Request<HelloRequest>> request) {
        return null;
    }

}
