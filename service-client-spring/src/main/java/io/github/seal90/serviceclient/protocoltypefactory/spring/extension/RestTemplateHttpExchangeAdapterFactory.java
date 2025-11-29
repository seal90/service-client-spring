package io.github.seal90.serviceclient.protocoltypefactory.spring.extension;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestTemplateAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.NAME_RESOLVED_FLAG;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

public class RestTemplateHttpExchangeAdapterFactory implements HttpExchangeAdapterFactory {

    private final ApplicationContext applicationContext;

    public RestTemplateHttpExchangeAdapterFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public HttpExchangeAdapter create(String serviceName, String channelName, String[] interceptors) {

        boolean nameResolved = false;
        RestTemplate restTemplate;
        if(channelName.startsWith("context://")) {
            restTemplate = (RestTemplate)applicationContext.getBean(channelName.replaceFirst("context://", ""));
            nameResolved = true;
        } else {
            RestTemplateBuilder builder = applicationContext.getBean(RestTemplateBuilder.class)
                .rootUri("http://"+serviceName);
            restTemplate = builder.build();
        }

        List<ClientHttpRequestInterceptor> interceptorList = new ArrayList<>();
        for(String interceptorName : interceptors) {
            ClientHttpRequestInterceptor interceptor = (ClientHttpRequestInterceptor)applicationContext.getBean(interceptorName);
            interceptorList.add(interceptor);
        }

        Boolean nameResolvedFlag = nameResolved;
        List<ClientHttpRequestInterceptor> requestInterceptors = restTemplate.getInterceptors();
        interceptorList.addAll(requestInterceptors);
        restTemplate.setInterceptors(interceptorList);
        restTemplate.getClientHttpRequestInitializers().addFirst(request -> {
            Map<String, Object> attributeMap = request.getAttributes();
            attributeMap.put(SERVICE_NAME, serviceName);
            attributeMap.put(CHANNEL_NAME, channelName);
            attributeMap.put(NAME_RESOLVED_FLAG, nameResolvedFlag);
        });

        return RestTemplateAdapter.create(restTemplate);
    }

}