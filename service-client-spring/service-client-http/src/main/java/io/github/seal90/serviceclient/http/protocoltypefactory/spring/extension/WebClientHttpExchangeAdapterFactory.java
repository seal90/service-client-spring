package io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension;

import io.github.seal90.serviceclient.core.ChannelNamePrefix;
import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.github.seal90.serviceclient.core.util.ApplicationContextBeanLookupUtils;
import io.github.seal90.serviceclient.http.ProtocolTypeHttpProperties;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.NAME_RESOLVED_FLAG;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

public class WebClientHttpExchangeAdapterFactory implements HttpExchangeAdapterFactory {

    private final ApplicationContext applicationContext;

    private final Environment environment;

    private final ProtocolTypeHttpProperties httpProtocolTypeProperties;

    public WebClientHttpExchangeAdapterFactory(ApplicationContext applicationContext, Environment environment
        , ProtocolTypeHttpProperties httpProtocolTypeProperties) {
      this.applicationContext = applicationContext;
      this.environment = environment;
      this.httpProtocolTypeProperties = httpProtocolTypeProperties;
    }

    @Override
    public HttpExchangeAdapter create(String serviceName, String channelName, String[] interceptors) {

      if(ChannelNamePrefix.isContext(channelName)) {
        if (interceptors.length > 0) {
          throw new BeanCreationException("interceptors are not allowed for channel name 'context'");
        }

        WebClient webClient = (WebClient)applicationContext.getBean(ChannelNamePrefix.extractContext(channelName));
        return WebClientAdapter.create(webClient);
      }

      List<ExchangeFilterFunction> exchangeFilterFunctions = new ArrayList<>();
      for(String interceptor: interceptors) {
        ExchangeFilterFunction exchangeFilterFunction = (ExchangeFilterFunction)applicationContext.getBean(interceptor);
        exchangeFilterFunctions.add(exchangeFilterFunction);
      }

      List<ExchangeFilterFunction> globalExchangeFilterFunction = ApplicationContextBeanLookupUtils
          .getBeansWithAnnotation(applicationContext, ExchangeFilterFunction.class, ServiceClientInterceptor.class);
      exchangeFilterFunctions.addAll(globalExchangeFilterFunction);

      ConfigResolve.NameResolveResult resolveResult = ConfigResolve.findAddress(serviceName, channelName, environment
          , httpProtocolTypeProperties);
      if (resolveResult.getAddress() == null ) {
        throw new BeanCreationException("Failed to resolve a valid service target address.");
      }

      WebClient.Builder builder = applicationContext.getBean(WebClient.Builder.class).baseUrl(resolveResult.getAddress())
          .filters((functions) -> {
            functions.addAll(exchangeFilterFunctions);
            AnnotationAwareOrderComparator.sort(functions);
            functions.addFirst(ExchangeFilterFunction.ofRequestProcessor(request -> {
              ClientRequest newRequest = ClientRequest.from(request)
                  .attributes(attributeMap -> {
                    attributeMap.put(SERVICE_NAME, serviceName);
                    attributeMap.put(CHANNEL_NAME, channelName);
                    attributeMap.put(NAME_RESOLVED_FLAG, resolveResult.resolved);
                  }).build();
          return Mono.just(newRequest);
        }));
      });

      return WebClientAdapter.create(builder.build());
    }

}