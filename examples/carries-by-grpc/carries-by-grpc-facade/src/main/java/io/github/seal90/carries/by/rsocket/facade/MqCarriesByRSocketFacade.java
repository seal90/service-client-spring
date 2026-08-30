package io.github.seal90.carries.by.rsocket.facade;

import org.springframework.messaging.handler.annotation.MessageMapping;

public interface MqCarriesByRSocketFacade {

    @MessageMapping("message.carries.by.rsocket.mq")
    default void mq(String requestMono){

    }

}
