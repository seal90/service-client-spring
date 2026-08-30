package io.github.seal90.carries.by.rsocket.facade;

import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Mono;

public interface MqCarriesByRSocketFacade {

    @MessageMapping("message.carries.by.rsocket.mq")
    default Mono<MessageResponse<HelloReply>> mq(Mono<MessageRequest<HelloRequest>> requestMono){
        return null;
    }

}
