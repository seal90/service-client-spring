package io.github.seal90.serviceclient.rsocket.core;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface RSocketFilterChain {
    Mono<Void> filter(RSocketExchange exchange);
}