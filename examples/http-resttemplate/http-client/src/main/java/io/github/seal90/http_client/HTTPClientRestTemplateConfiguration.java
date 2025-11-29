package io.github.seal90.http_client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateRequestCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;

import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

@Slf4j
@Configuration
public class HTTPClientRestTemplateConfiguration {

  @Bean
  public RestTemplateRequestCustomizer<?> requestRestTemplateRequestCustomizer() {
    return request -> {
      Map<String, Object> attributes = request.getAttributes();

      Object serviceName = attributes.get(SERVICE_NAME);
      Object channelName = attributes.get(CHANNEL_NAME);
      log.info("--- client interceptor serverName: {}, channelName: {}", serviceName, channelName);

      HttpHeaders httpHeaders = request.getHeaders();
      httpHeaders.add("CLIENT_TO_SERVER_HEADER_KEY", "CLIENT_TO_SERVER_HEADER_VALUE");
      httpHeaders.add("overlay-ns", "test");
    };
  }

  @Bean
  public RestTemplateCustomizer printResponseHeaderRestTemplateCustomizer() {

    return restTemplate -> restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor() {
      @Override
      public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        ClientHttpResponse clientHttpResponse = execution.execute(request, body);
        HttpHeaders httpHeaders = clientHttpResponse.getHeaders();

        String value = httpHeaders.getFirst("SERVER_TO_CLIENT_HEADER_KEY");
        log.info("--- client receive sever header SERVER_TO_CLIENT_HEADER_KEY : {}", value);
        return clientHttpResponse;
      }
    });
  }

}
