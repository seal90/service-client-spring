package io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension;

import io.github.seal90.serviceclient.core.ChannelNamePrefix;
import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.github.seal90.serviceclient.core.util.ApplicationContextBeanLookupUtils;
import io.github.seal90.serviceclient.http.ProtocolTypeHttpProperties;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestTemplateAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.NAME_RESOLVED_FLAG;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

public class RestTemplateHttpExchangeAdapterFactory implements HttpExchangeAdapterFactory {

  private final ApplicationContext applicationContext;

  private final Environment environment;

  private final ProtocolTypeHttpProperties httpProtocolTypeProperties;

  public RestTemplateHttpExchangeAdapterFactory(ApplicationContext applicationContext, Environment environment
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
      RestTemplate restTemplate = (RestTemplate)applicationContext.getBean(ChannelNamePrefix.extractContext(channelName));
      return RestTemplateAdapter.create(restTemplate);
    }

    List<ClientHttpRequestInterceptor> interceptorList = new ArrayList<>();
    for(String interceptorName : interceptors) {
      ClientHttpRequestInterceptor interceptor = (ClientHttpRequestInterceptor)applicationContext.getBean(interceptorName);
      interceptorList.add(interceptor);
    }

    List<ClientHttpRequestInterceptor> globalClientHttpRequestInterceptor = ApplicationContextBeanLookupUtils
        .getBeansWithAnnotation(applicationContext, ClientHttpRequestInterceptor.class, ServiceClientInterceptor.class);
    interceptorList.addAll(globalClientHttpRequestInterceptor);

    ConfigResolve.NameResolveResult resolveResult = ConfigResolve.findAddress(serviceName, channelName, environment, httpProtocolTypeProperties);
    if (resolveResult.getAddress() == null ) {
      throw new BeanCreationException("Failed to resolve a valid service target address.");
    }
    RestTemplateBuilder builder = applicationContext.getBean(RestTemplateBuilder.class)
        .rootUri(resolveResult.getAddress());

    RestTemplate restTemplate = builder.build();
    List<ClientHttpRequestInterceptor> requestInterceptors = restTemplate.getInterceptors();
    interceptorList.addAll(requestInterceptors);
    restTemplate.setInterceptors(interceptorList);

    restTemplate.getClientHttpRequestInitializers().addFirst(request -> {
      Map<String, Object> attributeMap = request.getAttributes();
      attributeMap.put(SERVICE_NAME, serviceName);
      attributeMap.put(CHANNEL_NAME, channelName);
      attributeMap.put(NAME_RESOLVED_FLAG, resolveResult.resolved);
    });

    return RestTemplateAdapter.create(restTemplate);
  }

}