package io.github.seal90.carries.by.rsocket;

import io.github.seal90.carries.by.rsocket.facade.HelloReply;
import io.github.seal90.carries.by.rsocket.facade.HelloRequest;
import io.github.seal90.carries.by.rsocket.facade.MqCarriesByRSocketFacade;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MqCarriesByRSocketConsumer implements MqCarriesByRSocketFacade {

    @Override
    public Mono<MessageResponse<HelloReply>> mq(Mono<MessageRequest<HelloRequest>> requestMono){
        return requestMono.map(request -> {

            HelloRequest helloRequest = request.getData(HelloRequest.class);
            log.info("consumer: {}", helloRequest.getName());
            return MessageResponse.success(HelloReply.newBuilder().build());
        });
    }
}
