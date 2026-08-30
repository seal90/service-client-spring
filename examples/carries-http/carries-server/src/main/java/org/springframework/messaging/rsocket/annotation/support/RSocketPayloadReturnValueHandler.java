/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.rsocket.annotation.support;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.rsocket.Payload;
import org.springframework.messaging.MessageHeaders;
//import org.springframework.messaging.rsocket.MetadataEncoder;
import org.springframework.messaging.rsocket.MetadataEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.reactive.AbstractEncoderMethodReturnValueHandler;
import org.springframework.messaging.rsocket.PayloadUtils;
import org.springframework.util.Assert;
import reactor.util.function.Tuple2;

/**
 * Extension of {@link AbstractEncoderMethodReturnValueHandler} that
 * {@link #handleEncodedContent handles} encoded content by wrapping data buffers
 * as RSocket payloads and by passing those through the {@link #RESPONSE_HEADER}
 * header.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketPayloadReturnValueHandler extends AbstractEncoderMethodReturnValueHandler {

	/**
	 * Message header name that is expected to have an {@link AtomicReference}
	 * which will receive the {@code Flux<Payload>} that represents the response.
	 */
	public static final String RESPONSE_HEADER = "rsocketResponse";

	public static final String RESPONSE_HEADER_HEADER = "rsocketResponseHeaders";

	public RSocketPayloadReturnValueHandler(List<Encoder<?>> encoders, ReactiveAdapterRegistry registry) {
		super(encoders, registry);
	}


	@Override
	protected Mono<Void> handleEncodedContent(
			Flux<DataBuffer> encodedContent, MethodParameter returnType, Message<?> message) {

		AtomicReference<Flux<Payload>> responseRef = getResponseReference(message);
		Assert.notNull(responseRef, "Missing '" + RESPONSE_HEADER + "'");

		MessageHeaders messageHeaders = message.getHeaders();
		Object headerValue = messageHeaders.get(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER);

		Assert.isInstanceOf(RSocketRequester.class, headerValue, "Expected header value of type RSocketRequester");
		RSocketRequester requester = (RSocketRequester) headerValue;

		// MimeType metadataMimeType, RSocketStrategies strategies
		AtomicReference<List<Tuple2<MimeType, Object>>> val = (AtomicReference<List<Tuple2<MimeType, Object>>>)messageHeaders.get(RESPONSE_HEADER_HEADER);
		List<Tuple2<MimeType, Object>> returnHeaders = val.get();

		MetadataEncoder encoder = new MetadataEncoder(requester.metadataMimeType(),requester.strategies());
		for(Tuple2<MimeType, Object> header : returnHeaders) {
			encoder.metadata(header.getT2(), header.getT1());
		}

		Mono<DataBuffer> headerDataBuffer =  encoder.encode();

		responseRef.set(headerDataBuffer.flatMapMany(metadata -> {
			return encodedContent.map(data -> PayloadUtils.createPayload(data, metadata));
		}));
//		responseRef.set(Flux.zip(encodedContent, Flux.from(headerDataBuffer))
//				.map(dataAndMetadata ->PayloadUtils.createPayload(dataAndMetadata.getT1(), dataAndMetadata.getT2())));
//		responseRef.set(encodedContent.map(PayloadUtils::createPayload));
		return Mono.empty();
	}

	@Override
	protected Mono<Void> handleNoContent(MethodParameter returnType, Message<?> message) {
		AtomicReference<Flux<Payload>> responseRef = getResponseReference(message);
		if (responseRef != null) {
			responseRef.set(Flux.empty());
		}
		return Mono.empty();
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private AtomicReference<Flux<Payload>> getResponseReference(Message<?> message) {
		Object headerValue = message.getHeaders().get(RESPONSE_HEADER);
		Assert.state(headerValue == null || headerValue instanceof AtomicReference, "Expected AtomicReference");
		return (AtomicReference<Flux<Payload>>) headerValue;
	}

}
