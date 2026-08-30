package io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket;

import io.github.seal90.serviceclient.carries.by.rsocket.context.CarriesConstant;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ExecMessageRSocket implements MessageRSocket {

    private RSocketRequester rSocketRequester;

    public ExecMessageRSocket(RSocketRequester rSocketRequester) {
        this.rSocketRequester = rSocketRequester;
    }

    @Override
    public Mono<Void> fireAndForget(Mono<MessageRequest> request) {
        return null;
    }

    @Override
    public Mono<MessageResponse> requestResponse(Mono<MessageRequest> request) {
        return request.flatMap(messageRequest -> {
            String serviceName = messageRequest.getAttributeStringValue(CarriesConstant.CONTEXT_TARGETSERVICENAME_ATTRIBUTE_KEY);
            String route = messageRequest.getAttributeStringValue(CarriesConstant.CONTEXT_ROUTE_ATTRIBUTE_KEY);

            Context.RpcRequest rpcRequest = Context.RpcRequest.newBuilder()
                .setTargetServiceName(serviceName)
                .setRoute(route)
                .putAllMetadata(messageRequest.getMetadata())
                .build();

            return rSocketRequester.route("message.carries.by.rsocket.requestResponse").metadata(metadataSpec -> {
                metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
            }).data(messageRequest).retrieveMono(MessageResponse.class);
        });
    }

    @Override
    public Flux<MessageResponse> requestStream(Mono<MessageRequest> request) {
        return null;
    }

    @Override
    public Flux<MessageResponse> requestChannel(Flux<MessageRequest> requests) {
        return requests.switchOnFirst((signal, messageRequestFlux) -> {
            if(!signal.hasValue()) {
                return signal.hasError()
                        ? Flux.<MessageResponse>error(signal.getThrowable())
                        : Flux.<MessageResponse>empty();
            }
            MessageRequest<?> messageRequest = signal.get();
            String serviceName = messageRequest.getAttributeStringValue(CarriesConstant.CONTEXT_TARGETSERVICENAME_ATTRIBUTE_KEY);
            String route = messageRequest.getAttributeStringValue(CarriesConstant.CONTEXT_ROUTE_ATTRIBUTE_KEY);

            Context.RpcRequest rpcRequest = Context.RpcRequest.newBuilder()
                    .setTargetServiceName(serviceName)
                    .setRoute(route)
                    .putAllMetadata(messageRequest.getMetadata())
                    .build();

            return rSocketRequester.route("message.carries.by.rsocket.stream").metadata(metadataSpec -> {
                metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
            }).data(Flux.concat(Flux.just(messageRequest), messageRequestFlux.skip(1))).retrieveFlux(MessageResponse.class);
        });
    }

    @Override
    public Mono<Void> metadataPush(Mono<MessageRequest> request) {
        return null;
    }
}
