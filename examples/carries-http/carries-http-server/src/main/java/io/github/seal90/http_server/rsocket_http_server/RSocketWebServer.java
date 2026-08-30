package io.github.seal90.http_server.rsocket_http_server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seal90.serviceclient.carries.http.extension.CarriesMetadata;
import io.github.seal90.serviceclient.carries.http.extension.HttpDownstreamContext;
import io.github.seal90.serviceclient.carries.http.extension.HttpExchangeProperties;
import io.github.seal90.serviceclient.carries.http.extension.HttpUpstreamContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.core.io.buffer.*;
import org.springframework.http.*;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.*;
import org.springframework.http.support.Netty4HeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.*;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ChannelOperationsId;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RSocketWebServer implements WebServer {

    private volatile CloseableChannel server;
    private volatile Thread awaitThread;
    private final HttpHandler httpHandler;
    private final MetadataExtractor metadataExtractor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final NettyDataBufferFactory factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    public RSocketWebServer(HttpHandler httpHandler, RSocketStrategies rsocketStrategies) {
        this.httpHandler = httpHandler;
        this.metadataExtractor = rsocketStrategies.metadataExtractor();
    }

    @Override
    public void start() throws WebServerException {
        if (this.server != null && !this.server.isDisposed()) {
            return;
        }

        SocketAcceptor acceptor = SocketAcceptor.with(new RSocket() {
            @Override
            public Mono<Payload> requestResponse(Payload payload) {
                // request
                Map<String, Object> parsedMetadata = metadataExtractor.extract(payload, MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.toString()));
                CarriesMetadata carriesMetadata = (CarriesMetadata) parsedMetadata.get(CarriesMetadata.CARRIES_METADATA_KEY);
                if (carriesMetadata == null) {
                    return Mono.error(new IllegalStateException("Missing carries metadata in RSocket composite metadata"));
                }
                HttpDownstreamContext httpDownstreamContext = objectMapper.convertValue(
                        carriesMetadata.getProtocolContext().get(HttpDownstreamContext.CarriesMetadata_KEY),
                        HttpDownstreamContext.class);
                HttpExchangeProperties httpExchange = httpDownstreamContext.getHttpExchange();
                // TODO io.github.seal90.serviceclient.carries.http.extension.CarriesHttpMethodInterceptor#invoke
                // CarriesMetadata -> HttpExchangeProperties
                ByteBuf rsocketReqData = payload.data();

                String id = UUID.randomUUID().toString();
                HttpMethod method = HttpMethod.valueOf(httpExchange.getMethod());
                URI uri = URI.create("/prefix"+httpExchange.getUrl());
                String contextPath = null;

                String[] exchangeHeaders = httpExchange.getHeaders();
                HttpHeaders headers = new HttpHeaders();
                headers.put("content-type", Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
//                headers.add();
                MultiValueMap<String, HttpCookie> cookies = MultiValueMap.fromSingleValue(new HashMap<>());
                Flux<DataBuffer> body = Flux.just(factory.wrap(rsocketReqData));
                Map<String, Object> attributes = new HashMap<>();

                // do invoke
                ServerHttpRequest request = new BuiltServerHttpRequest(id, method, uri, contextPath, headers, cookies, body, attributes);
                DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                HttpHeaders responseHeaders = new HttpHeaders();
                BuiltServerHttpResponse response = new BuiltServerHttpResponse(dataBufferFactory, responseHeaders);

                return httpHandler.handle(request, response).then(Mono.fromCallable(() -> {
                    HttpStatusCode statusCode = response.getStatusCode() != null ? response.getStatusCode() : HttpStatus.OK;
                    HttpHeaders respHeaders = response.getHeaders();;

                    HttpUpstreamContext upstreamContext = new HttpUpstreamContext();
                    upstreamContext.setCode(statusCode.value());
                    upstreamContext.setHeaders(respHeaders.asSingleValueMap());
                    CarriesMetadata upstreamCarriesMetadata = new CarriesMetadata();
                    upstreamCarriesMetadata.setProtocolContext(Map.of(HttpUpstreamContext.CarriesMetadata_KEY, upstreamContext));

                    byte[] metadataBytes = objectMapper.writeValueAsBytes(upstreamCarriesMetadata);
                    ByteBuf resultBuffer = response.getBodyAsByteBuf();
                    ByteBuf metadataBuffer = Unpooled.wrappedBuffer(metadataBytes);
                    return DefaultPayload.create(resultBuffer, metadataBuffer);
                }));
            }
        });

        try {
            this.server = RSocketServer.create(acceptor)
                    .bind(TcpServerTransport.create(10000))
                    .block();
            startAwaitThread();
        } catch (Exception e) {
            throw new WebServerException("Failed to start RSocket web server", e);
        }

    }

    private void startAwaitThread() {
        Thread thread = new Thread(() -> {
            CloseableChannel channel = this.server;
            try {
                if (channel != null) {
                    channel.onClose().block();
                }
            } catch (RuntimeException ex) {
                if (channel != null && !channel.isDisposed()) {
                    throw ex;
                }
            }
        }, "rsocket-web-server-awaiter");
        thread.setDaemon(false);
        thread.start();
        this.awaitThread = thread;
    }


    @Override
    public void stop() throws WebServerException {
        if (server != null && !server.isDisposed()) {
            server.dispose();
        }
        if (this.awaitThread != null) {
            this.awaitThread.interrupt();
            this.awaitThread = null;
        }
        this.server = null;
    }

    @Override
    public int getPort() {
        return 10000;
    }

    private static class BuiltServerHttpRequest implements ServerHttpRequest {
        private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");
        private final String id;
        private final HttpMethod method;
        private final URI uri;
        private final RequestPath path;
        private final MultiValueMap<String, String> queryParams;
        private final HttpHeaders headers;
        private final MultiValueMap<String, HttpCookie> cookies;
        private final Flux<DataBuffer> body;
        private final Map<String, Object> attributes;

        public BuiltServerHttpRequest(String id, HttpMethod method, URI uri, @Nullable String contextPath, HttpHeaders headers, MultiValueMap<String, HttpCookie> cookies, Flux<DataBuffer> body, Map<String, Object> attributes) {
            this.id = id;
            this.method = method;
            this.uri = uri;
            this.path = RequestPath.parse(uri, contextPath);
            this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
            this.cookies = unmodifiableCopy(cookies);
            this.queryParams = parseQueryParams(uri);
            this.body = body;
            this.attributes = attributes;
        }

        private static <K, V> MultiValueMap<K, V> unmodifiableCopy(MultiValueMap<K, V> original) {
            return CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap(original));
        }

        private static MultiValueMap<String, String> parseQueryParams(URI uri) {
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap();
            String query = uri.getRawQuery();
            String name;
            String value;
            if (query != null) {
                for(Matcher matcher = QUERY_PATTERN.matcher(query); matcher.find(); queryParams.add(name, value)) {
                    name = UriUtils.decode(matcher.group(1), StandardCharsets.UTF_8);
                    String eq = matcher.group(2);
                    value = matcher.group(3);
                    if (value != null) {
                        value = UriUtils.decode(value, StandardCharsets.UTF_8);
                    } else {
                        value = StringUtils.hasLength(eq) ? "" : null;
                    }
                }
            }

            return queryParams;
        }

        public String getId() {
            return this.id;
        }

        public HttpMethod getMethod() {
            return this.method;
        }

        public URI getURI() {
            return this.uri;
        }

        public Map<String, Object> getAttributes() {
            return this.attributes;
        }

        public RequestPath getPath() {
            return this.path;
        }

        public HttpHeaders getHeaders() {
            return this.headers;
        }

        public MultiValueMap<String, HttpCookie> getCookies() {
            return this.cookies;
        }

        public MultiValueMap<String, String> getQueryParams() {
            return this.queryParams;
        }

        public Flux<DataBuffer> getBody() {
            return this.body;
        }
    }

    private static class BuiltServerHttpResponse extends AbstractServerHttpResponse {
        private byte[] bodyBytes = new byte[0];

        public BuiltServerHttpResponse(DataBufferFactory dataBufferFactory, HttpHeaders headers) {
            super(dataBufferFactory, headers);
        }

        @Override
        public <T> T getNativeResponse() {
            return null;
        }

        @Override
        protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
            return DataBufferUtils.join(Flux.from(body))
                    .doOnNext(buffer -> {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        this.bodyBytes = bytes;
                        DataBufferUtils.release(buffer);
                    })
                    .then();
        }

        @Override
        protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body) {
            return writeWithInternal(Flux.from(body).concatMap(publisher -> publisher));
        }

        @Override
        protected void applyStatusCode() {

        }

        @Override
        protected void applyHeaders() {

        }

        @Override
        protected void applyCookies() {

        }

        public ByteBuf getBodyAsByteBuf() {
            return Unpooled.wrappedBuffer(this.bodyBytes);
        }
    }
}
