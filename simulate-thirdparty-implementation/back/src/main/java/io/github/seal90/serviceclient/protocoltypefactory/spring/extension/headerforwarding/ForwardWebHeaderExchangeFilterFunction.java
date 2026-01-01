package io.github.seal90.serviceclient.protocoltypefactory.spring.extension.headerforwarding;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.List;

@Data
@AllArgsConstructor
public class ForwardWebHeaderExchangeFilterFunction implements ExchangeFilterFunction {

  private String[] forwardWebHeaders;

  @Override
  public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
    if(forwardWebHeaders == null || forwardWebHeaders.length == 0) {
      return next.exchange(request);
    }

    return ReactiveRequestContextHolder.getRequest().flatMap(webRequest -> {
      HttpHeaders requestHeaders = request.headers();
      HttpHeaders webHeaders = webRequest.getHeaders();

      ClientRequest.Builder headerRequestBuilder = ClientRequest.from(request);

      for(String forwardWebHeader: forwardWebHeaders) {
        List<String> requestHeaderValues = requestHeaders.get(forwardWebHeader);
        if(requestHeaderValues == null || requestHeaderValues.isEmpty()) {
          List<String> webRequestHeaderValues = webHeaders.get(forwardWebHeader);
          if(webRequestHeaderValues != null && !webRequestHeaderValues.isEmpty()) {
            headerRequestBuilder.header(forwardWebHeader, webRequestHeaderValues.toArray(new String[0]));
          }
        }
      }

      return next.exchange(headerRequestBuilder.build());
    }).switchIfEmpty(Mono.defer(() -> next.exchange(request)));

  }

}
