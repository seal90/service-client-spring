package io.github.seal90.carries.by.rsocket.interceptor;

import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.MessageRSocket;
import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.MessageRSocketResponderInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LogMessageRSocketResponderInterceptor implements MessageRSocketResponderInterceptor {
    @Override
    public MessageRSocket apply(MessageRSocket messageRSocket) {
        return new MessageRSocket() {

            @Override
            public Mono<Void> fireAndForget(Mono<MessageRequest> request) {
                return messageRSocket.fireAndForget(request);
            }

            @Override
            public Mono<MessageResponse> requestResponse(Mono<MessageRequest> request) {
                return messageRSocket.requestResponse(request);
            }

            @Override
            public Flux<MessageResponse> requestStream(Mono<MessageRequest> request) {
                return messageRSocket.requestStream(request);
            }

            @Override
            public Flux<MessageResponse> requestChannel(Flux<MessageRequest> requests) {
                return messageRSocket.requestChannel(requests);
            }

            @Override
            public Mono<Void> metadataPush(Mono<MessageRequest> request) {
                return messageRSocket.metadataPush(request);
            }
        };
    }
}
