package io.github.seal90.http_client;

import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.boot.restclient.RestTemplateRequestCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;

import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

@Slf4j
@Configuration
public class HttpClientRestTemplateConfiguration {

  @Bean
  public RestTemplateRequestCustomizer<?> showWorkRestTemplateRequestCustomizer() {
    return request -> {
      log.info("The RestTemplateRequestCustomizer is still active and responsible for applying global settings.");
    };
  }

  @Bean
  public RestTemplateCustomizer showWrokRestTemplateCustomizer() {

    return restTemplate -> restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor() {
      @Override
      public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        log.info("The RestTemplateCustomizer is still active and responsible for applying global settings.");
        return execution.execute(request, body);
      }
    });
  }

  @Bean
  @ServiceClientInterceptor
  public ClientHttpRequestInterceptor serviceClientClientHttpRequestInterceptor() {
    return (HttpRequest request, byte[] body, ClientHttpRequestExecution execution) -> {
      log.info("ClientHttpRequestInterceptor @ServiceClientInterceptor work");

      Map<String, Object> attributes = request.getAttributes();

      Object serviceName = attributes.get(SERVICE_NAME);
      Object channelName = attributes.get(CHANNEL_NAME);
      log.info("--- client interceptor serverName: {}, channelName: {}", serviceName, channelName);

      HttpHeaders httpHeaders = request.getHeaders();
      httpHeaders.add("CLIENT_TO_SERVER_HEADER_KEY", "CLIENT_TO_SERVER_HEADER_VALUE");
      httpHeaders.add("overlay-ns", "test");


      ClientHttpResponse response = execution.execute(request, body);

      HttpHeaders responseHeaders = response.getHeaders();
      String value = responseHeaders.getFirst("SERVER_TO_CLIENT_HEADER_KEY");
      log.info("--- client receive sever header SERVER_TO_CLIENT_HEADER_KEY : {}", value);
      return response;
    };
  }

}
