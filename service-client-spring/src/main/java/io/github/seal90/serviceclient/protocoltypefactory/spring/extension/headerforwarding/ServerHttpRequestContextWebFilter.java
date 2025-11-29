package io.github.seal90.serviceclient.protocoltypefactory.spring.extension.headerforwarding;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class ServerHttpRequestContextWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
            .contextWrite(ctx -> ctx.put(ReactiveRequestContextHolder.CONTEXT_KEY, exchange.getRequest()));
    }
}