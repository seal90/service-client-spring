package io.github.seal90.serviceclient.carries.by.rsocket.extension;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.github.seal90.serviceclient.carries.by.rsocket.api.Request;
import io.github.seal90.serviceclient.carries.by.rsocket.api.Response;
import io.github.seal90.serviceclient.carries.by.rsocket.context.CarriesConstant;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.MessageRSocket;
import io.rsocket.Payload;
import io.rsocket.metadata.WellKnownMimeType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.transaction.reactive.TransactionContext;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import static io.github.seal90.serviceclient.carries.by.rsocket.context.CarriesConstant.CONTEXT_RESPONSE_METADATA_KEY;

@Slf4j
public class CarriesByRSocketMethodInterceptor implements MethodInterceptor {

    private String serviceName;
    private RSocketRequester rSocketRequester;
    private MessageRSocket messageRSocket;

    public CarriesByRSocketMethodInterceptor(String serviceName, RSocketRequester rSocketRequester, MessageRSocket messageRSocket) {
        this.serviceName = serviceName;
        this.rSocketRequester = rSocketRequester;
        this.messageRSocket = messageRSocket;
    }

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        Method invocationMethod = invocation.getMethod();
        AnnotationAttributes annotationAttributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
                invocationMethod, MessageMapping.class, true, true);

        String[] values = annotationAttributes.getStringArray("value");
        String route = values[0];


        Object[] invocationArguments = invocation.getArguments();
        Object invocationData = invocationArguments[0];
        Class<?> parameterType = invocationData.getClass();

        Class<?> returnType = invocationMethod.getReturnType();

        if(Mono.class.isAssignableFrom(parameterType) && Mono.class.isAssignableFrom(returnType)) {
            Class<?> parameterGenericType = ResolvableType.forMethodParameter(invocationMethod, 0).getGeneric(0).resolve();
            Class<?> returnGenericType = ResolvableType.forMethodReturnType(invocationMethod).as(Mono.class).getGeneric(0).resolve();

            if(MessageRequest.class.isAssignableFrom(parameterGenericType) && MessageResponse.class.isAssignableFrom(returnGenericType)) {
//                invocationMethod serviceName invocationData
                Mono<MessageRequest> requestMono = (Mono<MessageRequest>)invocationData;
                Mono<MessageRequest> requestMonoReq = Mono.deferContextual(ctx -> {
                    return requestMono.map((messageRequest -> {

                        Optional<TransactionContext> transactionContextOptional = ctx.getOrEmpty(TransactionContext.class);
                        if(transactionContextOptional.isPresent()) {
                            CarriesByRSocketTransactionManager.TransactionContext transactionContext = (CarriesByRSocketTransactionManager.TransactionContext)transactionContextOptional.get().getResources().get(CarriesByRSocketTransactionManager.TransactionContext.class);
                            messageRequest.putMetadata(CarriesConstant.CONTEXT_TRANSACTION_ID_METADATA_KEY, Any.pack(StringValue.of(transactionContext.getTransactionId())));
                        }

                        messageRequest.putAttribute(CarriesConstant.CONTEXT_TARGETSERVICENAME_ATTRIBUTE_KEY, serviceName);
                        messageRequest.putAttribute(CarriesConstant.CONTEXT_ROUTE_ATTRIBUTE_KEY, route);
                        messageRequest.putAttribute(CarriesConstant.CONTEXT_METHOD_ATTRIBUTE_KEY, invocationMethod);

                        return messageRequest;
                    }));
                });
//                requestMono = requestMono.map((messageRequest -> {
//
//                    messageRequest.putAttribute(CarriesConstant.CONTEXT_TARGETSERVICENAME_ATTRIBUTE_KEY, serviceName);
//                    messageRequest.putAttribute(CarriesConstant.CONTEXT_ROUTE_ATTRIBUTE_KEY, route);
//                    messageRequest.putAttribute(CarriesConstant.CONTEXT_METHOD_ATTRIBUTE_KEY, invocationMethod);
//
//                    return messageRequest;
//                }));
                return messageRSocket.requestResponse(requestMonoReq);


            }
        }

        if(Mono.class.isAssignableFrom(parameterType) && Flux.class.isAssignableFrom(returnType)) {
            Class<?> parameterGenericType = ResolvableType.forMethodParameter(invocationMethod, 0).getGeneric(0).resolve();
            Class<?> returnGenericType = ResolvableType.forMethodReturnType(invocationMethod).as(Flux.class).getGeneric(0).resolve();

            if(Request.class.equals(parameterGenericType) &&  Response.class.equals(returnGenericType)) {
                Mono<Request<?>> requestMono = (Mono<Request<?>>)invocationData;
                requestMono.map((requestInstance)->{
                    Map<String, Any> metadata = requestInstance.getMetadata();
                    Message data = requestInstance.getData();

                    Context.RpcRequest request = Context.RpcRequest.newBuilder()
                            .setTargetServiceName(serviceName)
                            .setRoute(values[0])
                            .putAllMetadata(metadata)
                            .build();

                    if(Response.class.equals(returnGenericType)) {
                        Class<?> dataGenericType = ResolvableType.forMethodReturnType(invocationMethod).as(Flux.class).getGeneric(0).getGeneric(0).resolve();

                        return rSocketRequester.route("message.carries.by.rsocket.requestStream").metadata(metadataSpec -> {
                                metadataSpec.metadata(request, CarriesConstant.REQUEST_METADATA_MIMETYPE);
                            }).data(data).retrieveMetadataFlux(dataGenericType).concatMap(tuple-> {
                                Message message = (Message)tuple.getT1();
                                Map<String, Object> responseMetadata = tuple.getT2();

                                if (!responseMetadata.isEmpty()) {
                                    Context.RpcResponse rpcResponse =
                                            (Context.RpcResponse) responseMetadata.get(CONTEXT_RESPONSE_METADATA_KEY);
                                    return Mono.just(Response.of(rpcResponse.getMetadataMap(), message));
                                } else {
                                    return Mono.just(Response.of(null, message));
                                }

                            })
                            .doOnError(err->{log.error("response error", err);});

//                        return rSocketRequester.route("message.carries.by.rsocket.requestResponse").metadata(metadataSpec -> {
//                                    metadataSpec.metadata(request, MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()));
//                                }).data(data).retrieveFlux(returnType)
//                                .doOnError(err->{log.error("response error", err);});
                    }
                    return Mono.error(()->{
                        throw new RuntimeException("unsupport");
                    });
                });
            } else {
                Context.RpcRequest request = Context.RpcRequest.newBuilder()
                        .setTargetServiceName(serviceName)
                        .setRoute(values[0])
                        .build();
                return rSocketRequester.route("message.carries.by.rsocket.requestStream").metadata(metadataSpec -> {
                            metadataSpec.metadata(request, CarriesConstant.REQUEST_METADATA_MIMETYPE);
                        }).data(invocationData).retrieveFlux(returnGenericType)
                        .doOnError(err->{log.error("response error", err);});
            }
        }

        if(Flux.class.isAssignableFrom(parameterType) && Flux.class.isAssignableFrom(returnType)) {
            Class<?> parameterGenericType = ResolvableType.forMethodParameter(invocationMethod, 0).getGeneric(0).resolve();
            Class<?> returnGenericType = ResolvableType.forMethodReturnType(invocationMethod).as(Flux.class).getGeneric(0).resolve();


            if(Request.class.equals(parameterGenericType) && Response.class.equals(returnGenericType)) {
                Class<? extends Message> dataGenericType = (Class<? extends Message>)ResolvableType.forMethodReturnType(invocationMethod).as(Flux.class).getGeneric(0).getGeneric().resolve();

                Flux<Request<? extends Message>> requestFlux = (Flux<Request<? extends Message>>)invocationData;

                return requestFlux.map(requestOperation -> {
                    return Context.Request.newBuilder().putAllMetadata(requestOperation.getMetadata()).setData(Any.pack(requestOperation.getData())).build();
                }).transformDeferred(flux -> {

                    Mono<Context.Request> firstRequest = flux.next().cache();

                    return firstRequest.flatMapMany(contextRequest -> {
                        Map<String,Any> metadata = contextRequest.getMetadataMap();
                        Context.RpcRequest request = Context.RpcRequest.newBuilder()
                            .setTargetServiceName(serviceName)
                            .setRoute(values[0])
                            .putAllMetadata(metadata)
                            .build();

                        Flux<Context.Request> emptyMetadataRequest = Flux.just(contextRequest).concatWith(flux).map(r->
                                Context.Request.newBuilder().setData(r.getData()
                                ).build());

                        return rSocketRequester.route("message.carries.by.rsocket.stream").metadata(metadataSpec -> {
                                metadataSpec.metadata(request, CarriesConstant.REQUEST_METADATA_MIMETYPE);
                            }).data(emptyMetadataRequest).retrieveMetadataFlux(Context.Response.class).concatMap(tuple-> {
                                Context.Response response = (Context.Response)tuple.getT1();
                                Map<String, Object> responseMetadata = tuple.getT2();
                                Message data = null;
                                try {
                                    data = response.getData().unpack(dataGenericType);
                                } catch (InvalidProtocolBufferException e) {
                                    throw new RuntimeException(e);
                                }
                                if (!responseMetadata.isEmpty()) {
                                    Context.RpcResponse rpcResponse =
                                            (Context.RpcResponse) responseMetadata.get(CONTEXT_RESPONSE_METADATA_KEY);
                                    return Mono.just(Response.of(rpcResponse.getMetadataMap(), data));
                                } else {
                                    return Mono.just(Response.of(null, data));
                                }

                            })
                            .doOnError(err->{log.error("response error", err);});
                    });
                });
            }

            if(Context.Request.class.isAssignableFrom(parameterGenericType) && Context.Response.class.isAssignableFrom(returnGenericType)) {
                Class<Context.Response> finalReturnGenericType = (Class<Context.Response>)returnGenericType;
                Flux<Context.Request> requestFlux = (Flux<Context.Request>)invocationData;
                return requestFlux.switchOnFirst((firstSignal, innerRequestFlux) -> {
                    if (!firstSignal.hasValue()) {
                        return innerRequestFlux;
                    }

                    Context.Request firstRequest = firstSignal.get();
                    Map<String, Any> metadata = firstRequest.getMetadataMap();
                    Context.RpcRequest rpcRequest = Context.RpcRequest.newBuilder()
                            .setTargetServiceName(serviceName)
                            .setRoute(values[0])
                            .putAllMetadata(metadata)
                            .build();

                    Flux<Context.Request> cleanedRequestFlux = innerRequestFlux
                            .map(rt -> Context.Request.newBuilder().setData(rt.getData()).build());
                    return rSocketRequester.route("message.carries.by.rsocket.stream").metadata(metadataSpec -> {
                        metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);})
                            .data(cleanedRequestFlux).retrieveMetadataFlux(finalReturnGenericType)
                            .switchOnFirst((firstRespSignal, innerResponseFlux) -> {
                                if (!firstRespSignal.hasValue()) {
                                    return innerResponseFlux.map(Tuple2::getT1);
                                }

                                Tuple2<Context.Response, Map<String, Object>> firstTuple = firstRespSignal.get();
                                Map<String, Object> responseMetadata = firstTuple.getT2();
                                Context.RpcResponse rpcResponse =
                                        (Context.RpcResponse) responseMetadata.get(CONTEXT_RESPONSE_METADATA_KEY);

                                Context.Response enrichedFirst;
                                if(rpcResponse != null) {
                                    enrichedFirst = firstTuple.getT1().toBuilder()
                                            .putAllMetadata(rpcResponse.getMetadataMap())
                                            .build();
                                } else {
                                    enrichedFirst = firstTuple.getT1();
                                }

                                Flux<Context.Response> remainingResponses = innerResponseFlux
                                        .skip(1)
                                        .map(Tuple2::getT1);

                                return Flux.just(enrichedFirst).concatWith(remainingResponses);
                            });

                });
            }

            if(MessageRequest.class.isAssignableFrom(parameterGenericType) && MessageResponse.class.isAssignableFrom(returnGenericType)) {
//                invocationMethod serviceName invocationData
                Flux<MessageRequest> requestFlux = (Flux<MessageRequest>)invocationData;
                requestFlux = requestFlux.switchOnFirst(((signal, messageRequestFlux) -> {
                    if(!signal.hasValue()) {
                        return messageRequestFlux;
                    }
                    MessageRequest messageRequest = signal.get();
                    messageRequest.putAttribute(CarriesConstant.CONTEXT_TARGETSERVICENAME_ATTRIBUTE_KEY, serviceName);
                    messageRequest.putAttribute(CarriesConstant.CONTEXT_ROUTE_ATTRIBUTE_KEY, route);
                    messageRequest.putAttribute(CarriesConstant.CONTEXT_METHOD_ATTRIBUTE_KEY, invocationMethod);

                    return Flux.concat(Flux.just(messageRequest), messageRequestFlux.skip(1));
                }));
                return messageRSocket.requestChannel(requestFlux);


            }
            Context.RpcRequest rpcRequest = Context.RpcRequest.newBuilder()
                    .setTargetServiceName(serviceName)
                    .setRoute(values[0])
//                    .putAllMetadata(metadata)
                    .build();

            return rSocketRequester.route("message.carries.by.rsocket.stream").metadata(metadataSpec -> {
                        metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);})
                    .data(invocationData).retrieveFlux(returnGenericType);
        }

        Context.RpcRequest request = Context.RpcRequest.newBuilder()
                .setTargetServiceName(serviceName)
                .setRoute(values[0])
                .build();
        return rSocketRequester.route("message.carries.by.rsocket.requestResponse").metadata(metadataSpec -> {
            metadataSpec.metadata(request, CarriesConstant.REQUEST_METADATA_MIMETYPE);
        }).data(invocationData).retrieveMono(returnType)
                .doOnError(err->{log.error("response error", err);}).block();
    }
}
