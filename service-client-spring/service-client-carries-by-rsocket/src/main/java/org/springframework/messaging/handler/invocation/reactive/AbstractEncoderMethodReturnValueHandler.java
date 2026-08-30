/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.invocation.reactive;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Any;
import io.github.seal90.serviceclient.carries.by.rsocket.api.Response;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import io.rsocket.Payload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Base class for a return value handler that encodes return values to
 * {@code Flux<DataBuffer>} through the configured {@link Encoder}s.
 *
 * <p>Subclasses must implement the abstract method
 * {@link #handleEncodedContent} to handle the resulting encoded content.
 *
 * <p>This handler should be ordered last since its {@link #supportsReturnType}
 * returns {@code true} for any method parameter type.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public abstract class AbstractEncoderMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private static final ResolvableType VOID_RESOLVABLE_TYPE = ResolvableType.forClass(Void.class);

	private static final ResolvableType OBJECT_RESOLVABLE_TYPE = ResolvableType.forClass(Object.class);

	private static final String COROUTINES_FLOW_CLASS_NAME = "kotlinx.coroutines.flow.Flow";


	protected final Log logger = LogFactory.getLog(getClass());

	private final List<Encoder<?>> encoders;

	private final ReactiveAdapterRegistry adapterRegistry;


	protected AbstractEncoderMethodReturnValueHandler(List<Encoder<?>> encoders, ReactiveAdapterRegistry registry) {
		Assert.notEmpty(encoders, "At least one Encoder is required");
		Assert.notNull(registry, "ReactiveAdapterRegistry is required");
		this.encoders = Collections.unmodifiableList(encoders);
		this.adapterRegistry = registry;
	}


	/**
	 * The configured encoders.
	 */
	public List<Encoder<?>> getEncoders() {
		return this.encoders;
	}

	/**
	 * The configured adapter registry.
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// We could check canEncode but we're probably last in order anyway
		return true;
	}

	@Override
	public Mono<Void> handleReturnValue(
			@Nullable Object returnValue, MethodParameter returnType, Message<?> message) {

		if (returnValue == null) {
			return handleNoContent(returnType, message);
		}

		DataBufferFactory bufferFactory = (DataBufferFactory) message.getHeaders()
				.getOrDefault(HandlerMethodReturnValueHandler.DATA_BUFFER_FACTORY_HEADER,
						DefaultDataBufferFactory.sharedInstance);

		MimeType mimeType = (MimeType) message.getHeaders().get(MessageHeaders.CONTENT_TYPE);

		ResolvableType returnValueType = ResolvableType.forMethodParameter(returnType);
		if(Flux.class.isAssignableFrom(returnValueType.resolve())) {
			ResolvableType returnValueGenericType = returnValueType.getGeneric();
			Class<?> resolveType = returnValueGenericType.resolve();
			if(Response.class.isAssignableFrom(resolveType)) {
				ResolvableType encodeGenericType = returnValueGenericType.getGeneric();
				ProtobufEncoder encoder = new ProtobufEncoder();
				Flux<Response<? extends com.google.protobuf.Message>> response = (Flux<Response<? extends com.google.protobuf.Message>>)returnValue;
				Flux<Tuple2<Map<String, Any>, DataBuffer>> encodedContent = response.index().map(tuple -> {
					long index = tuple.getT1();
					Response<? extends com.google.protobuf.Message> r = tuple.getT2();

					Context.Response cr = Context.Response.newBuilder().setData(Any.pack(r.getData())).build();
					DataBuffer encodeData = encoder.encodeValue(cr, bufferFactory, encodeGenericType, mimeType, Collections.emptyMap());
					if(index == 0) {
//						Map carriesResponseMetadata = (Map)message.getHeaders().get("carries_response_metadata");
//						carriesResponseMetadata.putAll(r.getMetadata());
						return Tuples.of(r.getMetadata(), encodeData);
					}
					return Tuples.of(Map.of(), encodeData);
//					return encoder.encodeValue(r.getData(), bufferFactory, encodeGenericType, mimeType, Collections.emptyMap());
				});

				return new ChannelSendOperator<>(encodedContent, publisher ->
						handleEncodedContent2(Flux.from(publisher), returnType, message));
			}

			if(Context.Response.class.isAssignableFrom(resolveType)) {
				ResolvableType encodeGenericType = returnValueGenericType.getGeneric();
				ProtobufEncoder encoder = new ProtobufEncoder();
				Flux<Context.Response> response = (Flux<Context.Response>)returnValue;
				Flux<Tuple2<Map<String, Any>, DataBuffer>> encodedContent =
						response.switchOnFirst((firstSignal, innerResponseFlux) -> {
						if (!firstSignal.hasValue()) {
							return Flux.empty();
						}

						Context.Response firstResponse = firstSignal.get();
						Map<String, Any> metadata = firstResponse.getMetadataMap();

						Context.Response firstResp = Context.Response.newBuilder()
								.setData(firstResponse.getData())
								.build();
						DataBuffer firstData = encoder.encodeValue(
								firstResp, bufferFactory, encodeGenericType, mimeType, Collections.emptyMap());
						Tuple2<Map<String, Any>, DataBuffer> firstTuple = Tuples.of(metadata, firstData);

						Flux<Tuple2<Map<String, Any>, DataBuffer>> remainingFlux = innerResponseFlux
								.skip(1)
								.map(otherResponse -> encoder.encodeValue(
										otherResponse, bufferFactory, encodeGenericType, mimeType, Collections.emptyMap()))
								.map(data -> Tuples.of(Map.of(), data));

						return Flux.just(firstTuple).concatWith(remainingFlux);
					});

//				Flux<Tuple2<Map<String, Any>, DataBuffer>> encodedContent = response.transformDeferred(responseFlux -> {
//					Mono<Context.Response> firstResponseMono = responseFlux.next().cache();
//					return firstResponseMono.flatMapMany(firstResponse -> {
//						Map<String, Any> metadata = firstResponse.getMetadataMap();
//						Context.Response firstResp = Context.Response.newBuilder().setData(firstResponse.getData()).build();
//						DataBuffer firstData = encoder.encodeValue(firstResp, bufferFactory, encodeGenericType, mimeType, Collections.emptyMap());
//
//						Flux<Tuple2<Map<String, Any>, DataBuffer>> otherFlux = responseFlux.map(otherData -> encoder.encodeValue(otherData, bufferFactory, encodeGenericType, mimeType, Collections.emptyMap()))
//								.map(data -> Tuples.of(Map.of(), data));
//						return Flux.just(Tuples.of(metadata, firstData)).concatWith(otherFlux);
//					});
//
//				});

				return new ChannelSendOperator<>(encodedContent, publisher ->
						handleEncodedContent2(Flux.from(publisher), returnType, message));
			}

			if(Payload.class.isAssignableFrom(resolveType)) {
				Flux<Payload> returnValueWithType = (Flux<Payload>)returnValue;
				return new ChannelSendOperator<>(returnValueWithType, publisher ->
						handleEncodedContent3(Flux.from(publisher), returnType, message));
			}
		}


		Flux<DataBuffer> encodedContent = encodeContent(
				returnValue, returnType, bufferFactory, mimeType, Collections.emptyMap());

		return new ChannelSendOperator<>(encodedContent, publisher ->
				handleEncodedContent(Flux.from(publisher), returnType, message));
	}

	private Flux<DataBuffer> encodeContent(
			@Nullable Object content, MethodParameter returnType, DataBufferFactory bufferFactory,
			@Nullable MimeType mimeType, Map<String, Object> hints) {

		ResolvableType returnValueType = ResolvableType.forMethodParameter(returnType);
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(returnValueType.resolve(), content);

		Publisher<?> publisher;
		ResolvableType elementType;
		if (adapter != null) {
			publisher = adapter.toPublisher(content);
			Method method = returnType.getMethod();
			boolean isUnwrapped = (method != null && KotlinDetector.isSuspendingFunction(method) &&
					!COROUTINES_FLOW_CLASS_NAME.equals(returnValueType.toClass().getName()));
			ResolvableType genericType = (isUnwrapped ? returnValueType : returnValueType.getGeneric());
			elementType = getElementType(adapter, genericType);
		}
		else {
			publisher = Mono.justOrEmpty(content);
			elementType = (returnValueType.toClass() == Object.class && content != null ?
					ResolvableType.forInstance(content) : returnValueType);
		}

		if (ClassUtils.isVoidType(elementType.resolve())) {
			return Flux.from(publisher).cast(DataBuffer.class);
		}

		Encoder<?> encoder = getEncoder(elementType, mimeType);
		return Flux.from(publisher).map(value ->
				encodeValue(value, elementType, encoder, bufferFactory, mimeType, hints));
	}

	private ResolvableType getElementType(ReactiveAdapter adapter, ResolvableType type) {
		if (adapter.isNoValue()) {
			return VOID_RESOLVABLE_TYPE;
		}
		else if (type != ResolvableType.NONE) {
			return type;
		}
		else {
			return OBJECT_RESOLVABLE_TYPE;
		}
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private <T> Encoder<T> getEncoder(ResolvableType elementType, @Nullable MimeType mimeType) {
		for (Encoder<?> encoder : getEncoders()) {
			if (encoder.canEncode(elementType, mimeType)) {
				return (Encoder<T>) encoder;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> DataBuffer encodeValue(
			Object element, ResolvableType elementType, @Nullable Encoder<T> encoder,
			DataBufferFactory bufferFactory, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		if (encoder == null) {
			encoder = getEncoder(ResolvableType.forInstance(element), mimeType);
			if (encoder == null) {
				throw new MessagingException(
						"No encoder for " + elementType + ", current value type is " + element.getClass());
			}
		}
		return encoder.encodeValue((T) element, bufferFactory, elementType, mimeType, hints);
	}

	/**
	 * Subclasses implement this method to handle encoded values in some way
	 * such as creating and sending messages.
	 * @param encodedContent the encoded content; each {@code DataBuffer}
	 * represents the fully-aggregated, encoded content for one value
	 * (i.e. payload) returned from the HandlerMethod.
	 * @param returnType return type of the handler method that produced the data
	 * @param message the input message handled by the handler method
	 * @return completion {@code Mono<Void>} for the handling
	 */
	protected abstract Mono<Void> handleEncodedContent(
			Flux<DataBuffer> encodedContent, MethodParameter returnType, Message<?> message);

	protected abstract Mono<Void> handleEncodedContent2(
			Flux<Tuple2<Map<String, Any>, DataBuffer>> encodedContent, MethodParameter returnType, Message<?> message);

	protected abstract Mono<Void> handleEncodedContent3(
			Flux<Payload> encodedContent, MethodParameter returnType, Message<?> message);
	/**
	 * Invoked for a {@code null} return value, which could mean a void method
	 * or method returning an async type parameterized by void.
	 * @param returnType return type of the handler method that produced the data
	 * @param message the input message handled by the handler method
	 * @return completion {@code Mono<Void>} for the handling
	 */
	protected abstract Mono<Void> handleNoContent(MethodParameter returnType, Message<?> message);

}
