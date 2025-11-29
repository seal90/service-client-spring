package io.github.seal90.serviceclient.protocoltypefactory.spring.extension;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.NAME_RESOLVED_FLAG;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

public class WebClientHttpExchangeAdapterFactory implements HttpExchangeAdapterFactory {

    private final ApplicationContext applicationContext;

    public WebClientHttpExchangeAdapterFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public HttpExchangeAdapter create(String serviceName, String channelName, String[] interceptors) {

      boolean nameResolved = false;
      WebClient webClient;
      if(channelName.startsWith("context://")) {
        webClient = (WebClient)applicationContext.getBean(channelName.replaceFirst("context://", ""));
        nameResolved = true;
      } else {
        WebClient.Builder builder = applicationContext.getBean(WebClient.Builder.class)
            .baseUrl("http://"+serviceName);
        webClient = builder.build();
      }

      List<ExchangeFilterFunction> exchangeFilterFunctions = new ArrayList<>();
      for(String interceptor: interceptors) {
        ExchangeFilterFunction exchangeFilterFunction = (ExchangeFilterFunction)applicationContext.getBean(interceptor);
        exchangeFilterFunctions.add(exchangeFilterFunction);
      }

      Boolean nameResolvedFlag = nameResolved;
      WebClient.Builder builder = webClient.mutate().filters((functions) -> {
        functions.addAll(exchangeFilterFunctions);
        AnnotationAwareOrderComparator.sort(functions);
        functions.addFirst(ExchangeFilterFunction.ofRequestProcessor(request -> {
          ClientRequest newRequest = ClientRequest.from(request)
              .attributes(attributeMap -> {
                attributeMap.put(SERVICE_NAME, serviceName);
                attributeMap.put(CHANNEL_NAME, channelName);
                attributeMap.put(NAME_RESOLVED_FLAG, nameResolvedFlag);
              })
              .build();
          return Mono.just(newRequest);
        }));
      });

      return WebClientAdapter.create(builder.build());
    }
}