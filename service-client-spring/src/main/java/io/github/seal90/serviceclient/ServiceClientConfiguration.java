package io.github.seal90.serviceclient;

import feign.Feign;
import io.github.seal90.serviceclient.protocoltypefactory.FeignProtocolTypeFactory;
import io.github.seal90.serviceclient.protocoltypefactory.GrpcProtocolTypeFactory;
import io.github.seal90.serviceclient.protocoltypefactory.HttpProtocolTypeFactory;
import io.github.seal90.serviceclient.protocoltypefactory.grpc.extension.metadataforwarding.ForwardGrpcMetadataClientInterceptor;
import io.github.seal90.serviceclient.protocoltypefactory.grpc.extension.metadataforwarding.ForwardGrpcMetadataServerInterceptor;
import io.github.seal90.serviceclient.protocoltypefactory.spring.extension.RestTemplateHttpExchangeAdapterFactory;
import io.github.seal90.serviceclient.protocoltypefactory.spring.extension.WebClientHttpExchangeAdapterFactory;
import io.github.seal90.serviceclient.protocoltypefactory.spring.extension.headerforwarding.ForwardWebHeaderExchangeFilterFunction;
import io.github.seal90.serviceclient.protocoltypefactory.spring.extension.headerforwarding.ForwardWebHeaderRequestInterceptor;
import io.github.seal90.serviceclient.protocoltypefactory.spring.extension.headerforwarding.ServerHttpRequestContextWebFilter;
import io.github.seal90.serviceclient.protocoltypefactory.spring.extension.nameresovle.NameResolveClientHttpRequestInterceptor;
import io.github.seal90.serviceclient.protocoltypefactory.spring.extension.nameresovle.NameResolveExchangeFilterFunction;
import io.grpc.ClientInterceptor;
import io.grpc.Grpc;
import io.grpc.ServerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * ServiceClient configuration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ServiceClientProperties.class, GrpcProtocolTypeProperties.class, HttpProtocolTypeProperties.class})
public class ServiceClientConfiguration {

  @ConditionalOnClass(Grpc.class)
  public static class GrpcConfig {

    @Bean
    public ProtocolTypeFactory grpcProtocolTypeFactory() {
      return new GrpcProtocolTypeFactory();
    }

    @Bean
    @GlobalClientInterceptor
    public ClientInterceptor forwardGrpcMetadataClientInterceptor(GrpcProtocolTypeProperties protocolTypeProperties) {
      return new ForwardGrpcMetadataClientInterceptor(protocolTypeProperties.getForwardGrpcMetadata());
    }

    @Bean
    @GlobalServerInterceptor
    public ServerInterceptor forwardGrpcMetadataServerInterceptor() {
      return new ForwardGrpcMetadataServerInterceptor();
    }
  }

  @ConditionalOnClass(WebClient.class)
  public static class ReactiveWebConfig {

    @Bean
    public WebFilter serverHttpRequestContextWebFilter() {
      return new ServerHttpRequestContextWebFilter();
    }

    @Bean
    public WebClientCustomizer serviceClientWebClientCustomizer(HttpProtocolTypeProperties httpProtocolTypeProperties) {
      return (builder) -> {
        builder.filter(new NameResolveExchangeFilterFunction(httpProtocolTypeProperties));
        builder.filter(new ForwardWebHeaderExchangeFilterFunction(httpProtocolTypeProperties.getForwardWebHeaders()));
      };
    }

    @Bean
    @ConditionalOnMissingBean(HttpProtocolTypeFactory.class)
    public ProtocolTypeFactory hTTPProtocolTypeFactory(ApplicationContext applicationContext) {
      return new HttpProtocolTypeFactory(new WebClientHttpExchangeAdapterFactory(applicationContext));
    }
  }

  @ConditionalOnClass(RestTemplate.class)
  @ConditionalOnMissingClass("org.springframework.web.reactive.function.client.WebClient")
  public static class BlockingWebConfig {

    @Bean
    public RestTemplateCustomizer serviceClientRestTemplateCustomizer(HttpProtocolTypeProperties httpProtocolTypeProperties) {
      return (restTemplate) -> {
        List<ClientHttpRequestInterceptor> list = new ArrayList<>(restTemplate.getInterceptors());
        list.add(new NameResolveClientHttpRequestInterceptor(httpProtocolTypeProperties));
        list.add(new ForwardWebHeaderRequestInterceptor(httpProtocolTypeProperties.getForwardWebHeaders()));
        restTemplate.setInterceptors(list);
      };
    }

    @Bean
    @ConditionalOnMissingBean(HttpProtocolTypeFactory.class)
    public ProtocolTypeFactory hTTPProtocolTypeFactory(ApplicationContext applicationContext) {
      return new HttpProtocolTypeFactory(new RestTemplateHttpExchangeAdapterFactory(applicationContext));
    }
  }

  @Bean
  @ConditionalOnClass(Feign.class)
  public ProtocolTypeFactory feignProtocolTypeFactory() {
    return new FeignProtocolTypeFactory();
  }

}
