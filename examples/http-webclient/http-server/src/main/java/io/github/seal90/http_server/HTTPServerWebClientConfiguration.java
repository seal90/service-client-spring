package io.github.seal90.http_server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class HTTPServerWebClientConfiguration {

  @Bean
  public WebFilter webFilter() {
    return new WebFilter() {
      @Override
      public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders httpHeaders = request.getHeaders();
        List<String> value = httpHeaders.get("CLIENT_TO_SERVER_HEADER_KEY");
        log.info("--- server receive client header CLIENT_TO_SERVER_HEADER_KEY : {}", value);
        List<String> overlyNS = httpHeaders.get("overlay-ns");
        log.info("--- server receive client header overlay-ns : {}", overlyNS);

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().addAll("SERVER_TO_CLIENT_HEADER_KEY", Collections.singletonList("SERVER_TO_CLIENT_HEADER_VALUE"));

        return chain.filter(exchange).flatMap((aVoid) -> {
          // not work
//          ServerHttpResponse response = exchange.getResponse();
//          response.getHeaders().addAll("SERVER_TO_CLIENT_HEADER_KEY", Collections.singletonList("SERVER_TO_CLIENT_HEADER_VALUE"));
          return Mono.just(aVoid);
        });
      }
    };
  }
}
