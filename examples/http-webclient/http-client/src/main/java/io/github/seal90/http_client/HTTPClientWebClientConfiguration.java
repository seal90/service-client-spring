package io.github.seal90.http_client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

@Slf4j
@Configuration
public class HTTPClientWebClientConfiguration {

  @Bean
  public WebClientCustomizer headerDealWebClientCustomizer() {
    return builder -> builder.filter((ClientRequest request, ExchangeFunction next) -> {
      Map<String, Object> attributes = request.attributes();

      Object serviceName = attributes.get(SERVICE_NAME);
      Object channelName = attributes.get(CHANNEL_NAME);
      log.info("--- client interceptor serverName: {}, channelName: {}", serviceName, channelName);

      ClientRequest.Builder headerRequestBuilder = ClientRequest.from(request);
      headerRequestBuilder.header("CLIENT_TO_SERVER_HEADER_KEY", "CLIENT_TO_SERVER_HEADER_VALUE");
      headerRequestBuilder.header("overlay-ns", "test");

      Mono<ClientResponse> responseMono = next.exchange(headerRequestBuilder.build());
      return responseMono.flatMap(response -> {

        ClientResponse.Headers headers = response.headers();
        List<String> value = headers.header("SERVER_TO_CLIENT_HEADER_KEY");
        log.info("--- client receive sever header SERVER_TO_CLIENT_HEADER_KEY : {}", value);

        return Mono.just(response);
      });
    });
  }
}
