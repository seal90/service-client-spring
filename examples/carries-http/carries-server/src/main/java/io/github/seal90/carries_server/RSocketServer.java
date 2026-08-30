package io.github.seal90.carries_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seal90.serviceclient.carries.http.extension.CarriesMetadata;
import io.github.seal90.serviceclient.carries.http.extension.HttpDownstreamContext;
import io.github.seal90.serviceclient.carries.http.extension.HttpExchangeProperties;
import io.github.seal90.serviceclient.carries.http.extension.HttpUpstreamContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.support.RSocketPayloadReturnValueHandler;
import org.springframework.messaging.rsocket.service.RSocketExchange;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
public class RSocketServer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RSocketRequester rSocketRequester;

    @RSocketExchange("message.http.requestResponse")
    public Mono<DataBuffer> carriesHttp(@Header(CarriesMetadata.CARRIES_METADATA_KEY) CarriesMetadata carriesMetadata,
                            @Header(RSocketPayloadReturnValueHandler.RESPONSE_HEADER_HEADER) AtomicReference<List<Tuple2<MimeType, Object>>> responseHeadersRef,
                            @Payload(required = false) Mono<DataBuffer> body) throws JsonProcessingException {

        Object metadataMap = carriesMetadata.getProtocolContext().get(HttpDownstreamContext.CarriesMetadata_KEY);
        HttpDownstreamContext downstreamContext = objectMapper.convertValue(metadataMap, HttpDownstreamContext.class);
//        HttpDownstreamContext downstreamContext = objectMapper.readValue(objectMapper.writeValueAsString(metadataMap), HttpDownstreamContext.class);

//        HttpDownstreamContext downstreamContext = (HttpDownstreamContext)carriesMetadata.getProtocolContext().get(HttpDownstreamContext.CarriesMetadata_KEY);
        HttpExchangeProperties httpExchange = downstreamContext.getHttpExchange();
        String serviceName = httpExchange.getServiceName();
        log.info("serviceName: {}", serviceName);

        String baseUrl = "http://127.0.0.1:8080/prefix" + httpExchange.getUrl();

        return WebClient.builder().baseUrl(baseUrl).build()
                .method(HttpMethod.valueOf(httpExchange.getMethod())).contentType(MediaType.APPLICATION_JSON)
//                .headers((headers) -> headers.setAll(proxyHeaders))
                .body(body, DataBuffer.class)
//                .body(Mono.just("{}"), String.class)
                .exchangeToMono(resp -> {
                    Map<String, String> bizReturnMap = new HashMap<>(resp.headers().asHttpHeaders().toSingleValueMap());

                    HttpUpstreamContext upstreamContext = new HttpUpstreamContext();
                    upstreamContext.setHeaders(bizReturnMap);
                    CarriesMetadata upstreamCarriesMetadata = new CarriesMetadata();
                    upstreamCarriesMetadata.setProtocolContext(Map.of(HttpUpstreamContext.CarriesMetadata_KEY, upstreamContext));

                    List<Tuple2<MimeType, Object>> returnHeaders = responseHeadersRef.get();
                    returnHeaders.add(Tuples.of(MediaType.APPLICATION_JSON, upstreamCarriesMetadata));
                    return resp.bodyToMono(DataBuffer.class);
                });
    }

    @RSocketExchange("message.http.proxy.requestResponse")
    public Mono<DataBuffer> carriesHttpProxy(@Header(CarriesMetadata.CARRIES_METADATA_KEY) CarriesMetadata carriesMetadata,
                                                     @Header(RSocketPayloadReturnValueHandler.RESPONSE_HEADER_HEADER) AtomicReference<List<Tuple2<MimeType, Object>>> responseHeadersRef,
                                                     @Payload(required = false) Mono<DataBuffer> body) {

        return body.flatMap(dataBuffer -> {
            return rSocketRequester.route("message.http.proxy.requestResponse").metadata(metadataSpec->{
                metadataSpec.metadata(carriesMetadata, MimeTypeUtils.APPLICATION_JSON);
            }).data(dataBuffer).retrieveMono(DataBuffer.class);
        });

    }
}
