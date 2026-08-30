package io.github.seal90.http_server;

import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

public class ReactiveRequestContextHolder {
    public static final Class<ServerHttpRequest> CONTEXT_KEY = ServerHttpRequest.class;

    public static Mono<ServerHttpRequest> getRequest() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(CONTEXT_KEY)) {
                return Mono.just(ctx.get(CONTEXT_KEY));
            }
            return Mono.error(new IllegalStateException("No ServerWebExchange in context"));
        });
    }
}