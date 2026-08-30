package io.github.seal90.carries.by.rsocket.facade;

import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HelloWorldWithMessageBoxFacade {

    @MessageMapping("rsocket.msg.message.box.sayHello")
    default Mono<MessageResponse<HelloReply>> sayHello(Mono<MessageRequest<HelloRequest>> request) {
        return null;
    }

    @MessageMapping("rsocket.msg.message.box.sayHelloAllFlux")
    default Flux<MessageResponse<HelloReply>> sayHelloAllFlux(Flux<MessageRequest<HelloRequest>> request) {
        return null;
    }

}
