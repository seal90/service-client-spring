package io.github.seal90.http_client;

import feign.RequestInterceptor;
import io.github.seal90.serviceclient.protocoltypefactory.feign.extension.GlobalRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

@Slf4j
@Configuration
public class HTTPClientFeignConfiguration {

  @Bean
  @GlobalRequestInterceptor
  public RequestInterceptor requestInterceptor() {
    return new RequestInterceptor() {
      @Override
      public void apply(feign.RequestTemplate requestTemplate) {
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Collection<String> serviceName = headers.get(SERVICE_NAME);
        Collection<String> channelName = headers.get(CHANNEL_NAME);
        log.info("--- client interceptor serverName: {}, channelName: {}", serviceName, channelName);

        headers.put("CLIENT_TO_SERVER_HEADER_KEY", Collections.singleton("CLIENT_TO_SERVER_HEADER_VALUE"));
        headers.put("overlay-ns", Collections.singleton("test"));
      }
    };
  }
}
