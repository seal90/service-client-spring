package io.github.seal90.carries.by.rsocket;

import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.MessageRSocket;
import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.MessageRSocketRequesterInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LogMessageRSocketRequesterInterceptor implements MessageRSocketRequesterInterceptor {
    @Override
    public MessageRSocket apply(MessageRSocket messageRSocket) {
        return new MessageRSocket(){

            @Override
            public Mono<Void> fireAndForget(Mono<MessageRequest> request) {
                return messageRSocket.fireAndForget(request);
            }

            @Override
            public Mono<MessageResponse> requestResponse(Mono<MessageRequest> request) {
                log.info("LogMessageRSocketRequesterInterceptor requestResponse ");
                return messageRSocket.requestResponse(request);
            }

            @Override
            public Flux<MessageResponse> requestStream(Mono<MessageRequest> request) {
                return messageRSocket.requestStream(request);
            }

            @Override
            public Flux<MessageResponse> requestChannel(Flux<MessageRequest> requests) {
                log.info("LogMessageRSocketRequesterInterceptor requestChannel ");
                return messageRSocket.requestChannel(requests);
            }

            @Override
            public Mono<Void> metadataPush(Mono<MessageRequest> request) {
                return messageRSocket.metadataPush(request);
            }
        };
    }
}
