package io.github.seal90.carries.by.rsocket.facade;

import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// metadata in arguments
public interface HelloWorldWithMetadataFacade {

    // Carry metadata in the response tuple?
    @MessageMapping("rsocket.msg.metadata.sayHello")
    default HelloReply sayHello(HelloRequest request, Context.RpcMetadata... rpcMetadata) {
        return null;
    }

    @MessageMapping("rsocket.msg.metadata.requestStream.sayHello")
    default Flux<HelloReply> sayHello(Mono<HelloRequest> request, Context.RpcMetadata... rpcMetadata) {
        return null;
    }
}
