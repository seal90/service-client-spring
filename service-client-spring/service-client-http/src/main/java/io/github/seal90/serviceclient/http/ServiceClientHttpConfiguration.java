package io.github.seal90.serviceclient.http;

import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.github.seal90.serviceclient.http.protocoltypefactory.ProtocolTypeHttpFactory;
import io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.RestTemplateHttpExchangeAdapterFactory;
import io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.WebClientHttpExchangeAdapterFactory;
import io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.headerforwarding.ForwardMetadataExchangeFilterFunction;
import io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.headerforwarding.ForwardMetadataRequestInterceptor;
import io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.headerforwarding.ServerHttpRequestContextWebFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;

/**
 * ServiceClient configuration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ProtocolTypeHttpProperties.class})
public class ServiceClientHttpConfiguration {

  @ConditionalOnClass(WebClient.class)
  public static class ReactiveWebConfig {

    @Bean
    public WebFilter serverHttpRequestContextWebFilter() {
      return new ServerHttpRequestContextWebFilter();
    }

//    @Bean
//    public WebClientCustomizer serviceClientWebClientCustomizer(ProtocolTypeHttpProperties httpProtocolTypeProperties) {
//      return (builder) -> {
//        builder.filter(new NameResolveExchangeFilterFunction(httpProtocolTypeProperties));
//        builder.filter(new ForwardMetadataExchangeFilterFunction(httpProtocolTypeProperties.getForwardWebHeaders()));
//      };
//    }

    @Bean
    @ServiceClientInterceptor
    public ExchangeFilterFunction forwardWebHeaderExchangeFilterFunction(ProtocolTypeHttpProperties httpProtocolTypeProperties) {
      return new ForwardMetadataExchangeFilterFunction(httpProtocolTypeProperties.getForwardMetadata());
    }

    @Bean
    @ConditionalOnMissingBean(ProtocolTypeHttpFactory.class)
    public ProtocolTypeFactory protocolTypeHttpFactory(ApplicationContext applicationContext, Environment environment
        , ProtocolTypeHttpProperties httpProtocolTypeProperties) {
      return new ProtocolTypeHttpFactory(new WebClientHttpExchangeAdapterFactory(applicationContext, environment
          , httpProtocolTypeProperties));
    }
  }

  @ConditionalOnClass(RestTemplate.class)
  @ConditionalOnMissingClass("org.springframework.web.reactive.function.client.WebClient")
  public static class BlockingWebConfig {

//    @Bean
//    public RestTemplateCustomizer serviceClientRestTemplateCustomizer(ProtocolTypeHttpProperties httpProtocolTypeProperties) {
//      return (restTemplate) -> {
//        List<ClientHttpRequestInterceptor> list = new ArrayList<>(restTemplate.getInterceptors());
//        list.add(new NameResolveClientHttpRequestInterceptor(httpProtocolTypeProperties));
//        list.add(new ForwardMetadataRequestInterceptor(httpProtocolTypeProperties.getForwardWebHeaders()));
//        restTemplate.setInterceptors(list);
//      };
//    }

    @Bean
    @ServiceClientInterceptor
    public ClientHttpRequestInterceptor forwardMetadataRequestInterceptor(ProtocolTypeHttpProperties httpProtocolTypeProperties) {
      return new ForwardMetadataRequestInterceptor(httpProtocolTypeProperties.getForwardMetadata());
    }

    @Bean
    @ConditionalOnMissingBean(ProtocolTypeHttpFactory.class)
    public ProtocolTypeFactory protocolTypeHttpFactory(ApplicationContext applicationContext, Environment environment
        , ProtocolTypeHttpProperties httpProtocolTypeProperties) {
      return new ProtocolTypeHttpFactory(new RestTemplateHttpExchangeAdapterFactory(applicationContext, environment
          , httpProtocolTypeProperties));
    }
  }

}
