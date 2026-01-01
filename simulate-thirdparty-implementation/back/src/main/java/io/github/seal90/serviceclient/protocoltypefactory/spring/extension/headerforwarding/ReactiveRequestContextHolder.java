package io.github.seal90.serviceclient.protocoltypefactory.spring.extension.headerforwarding;

import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

public class ReactiveRequestContextHolder {

    public static final String CONTEXT_KEY = ReactiveRequestContextHolder.class.getName();

    public static Mono<ServerHttpRequest> getRequest() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(CONTEXT_KEY)) {
                return Mono.just(ctx.get(CONTEXT_KEY));
            }
            return Mono.empty();
        });
    }
}