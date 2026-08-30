package io.github.seal90.carries_server;

import io.github.seal90.serviceclient.carries.by.rsocket.context.CarriesConstant;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import io.rsocket.metadata.WellKnownMimeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.support.RSocketPayloadReturnValueHandler;
import org.springframework.messaging.rsocket.service.RSocketExchange;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Controller
public class CarriesByRSocketController {

    @Autowired
    private RSocketRequester.Builder rSocketRequesterBuilder;

    @RSocketExchange("message.carries.by.rsocket.requestResponse")
    public Mono<DataBuffer> carriesByRSocket(@Header(CarriesConstant.CONTEXT_REQUEST_METADATA_KEY) Context.RpcRequest rpcRequest,
                                             @Header(RSocketPayloadReturnValueHandler.RESPONSE_HEADER_HEADER) AtomicReference<List<Tuple2<MimeType, Object>>> responseHeadersRef,
                                             @Payload(required = false) Mono<DataBuffer> body) {

        String targetServiceName = rpcRequest.getTargetServiceName();
        // mock
        if("mq-carries-by-rsocket".equals(targetServiceName) || "mysql-crud-by-rsocket".equals(targetServiceName)) {
            RSocketRequester rSocketRequester = rSocketRequesterBuilder
                    .dataMimeType(MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()))
                    .tcp("localhost",9898);
            return body.flatMap(dataBuffer -> {
                return rSocketRequester.route(rpcRequest.getRoute()).metadata(metadataSpec->{
                    metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
                }).data(dataBuffer).retrieveMono(DataBuffer.class);
            });
        }

        RSocketRequester rSocketRequester = rSocketRequesterBuilder
                .dataMimeType(MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()))
                .tcp("localhost",20001);
        return body.flatMap(dataBuffer -> {
            return rSocketRequester.route(rpcRequest.getRoute()).metadata(metadataSpec->{
                metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
            }).data(dataBuffer).retrieveMono(DataBuffer.class);
        });

    }

    @RSocketExchange("message.carries.by.rsocket.requestStream")
    public Flux<DataBuffer> carriesByRSocketRequestStream(@Header(CarriesConstant.CONTEXT_REQUEST_METADATA_KEY) Context.RpcRequest rpcRequest,
//                                             @Header(RSocketPayloadReturnValueHandler.RESPONSE_HEADER_HEADER) AtomicReference<List<Tuple2<MimeType, Object>>> responseHeadersRef,
                                             @Payload(required = false) Mono<DataBuffer> body) {

        String targetServiceName = rpcRequest.getTargetServiceName();

        RSocketRequester rSocketRequester = rSocketRequesterBuilder
                .dataMimeType(MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()))
                .tcp("localhost",20001);
        return body.flatMapMany(dataBuffer -> {
            return rSocketRequester.route(rpcRequest.getRoute()).metadata(metadataSpec->{
                metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
            }).data(dataBuffer).retrieveFlux(DataBuffer.class).doOnDiscard(DataBuffer.class, DataBufferUtils::release);
        });

    }

    @RSocketExchange("message.carries.by.rsocket.stream")
    public Flux<io.rsocket.Payload> carriesByRSocketStream(@Header(CarriesConstant.CONTEXT_REQUEST_METADATA_KEY) Context.RpcRequest rpcRequest,
                                                   @Header(RSocketPayloadReturnValueHandler.RESPONSE_HEADER_HEADER) AtomicReference<List<Tuple2<MimeType, Object>>> responseHeadersRef,
                                                   @Payload(required = false) Flux<DataBuffer> body) {

        String targetServiceName = rpcRequest.getTargetServiceName();

        RSocketRequester rSocketRequester = rSocketRequesterBuilder
                .dataMimeType(MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()))
                .tcp("localhost",20001);
        return rSocketRequester.route(rpcRequest.getRoute()).metadata(metadataSpec->{
            metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
        }).data(body).retrieveFlux(io.rsocket.Payload.class);

    }
}
