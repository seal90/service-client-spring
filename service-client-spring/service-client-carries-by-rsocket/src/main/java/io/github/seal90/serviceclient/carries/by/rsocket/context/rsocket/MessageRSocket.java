package io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket;

import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageRSocket {

    Mono<Void> fireAndForget(Mono<MessageRequest> request);

    Mono<MessageResponse> requestResponse(Mono<MessageRequest> request);

    Flux<MessageResponse> requestStream(Mono<MessageRequest> request);

    Flux<MessageResponse> requestChannel(Flux<MessageRequest> requests);

    Mono<Void> metadataPush(Mono<MessageRequest> request);

}
