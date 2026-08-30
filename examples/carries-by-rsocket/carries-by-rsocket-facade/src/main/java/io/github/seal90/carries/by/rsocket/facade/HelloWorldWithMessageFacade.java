package io.github.seal90.carries.by.rsocket.facade;

import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;

public interface HelloWorldWithMessageFacade {

    @MessageMapping("rsocket.msg.message.sayHelloAllFlux")
    default Flux<Context.Response> sayHelloAllFlux(Flux<Context.Request> request) {
        return null;
    }

}
