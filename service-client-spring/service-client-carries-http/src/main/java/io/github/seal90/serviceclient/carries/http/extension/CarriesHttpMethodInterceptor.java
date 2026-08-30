package io.github.seal90.serviceclient.carries.http.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.Payload;
import io.rsocket.core.RSocketClient;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class CarriesHttpMethodInterceptor implements MethodInterceptor {

    private String serviceName;
    private RSocketRequester rSocketRequester;
    private ObjectMapper objectMapper = new ObjectMapper();

    public CarriesHttpMethodInterceptor(String serviceName, RSocketRequester rSocketRequester) {
        this.serviceName = serviceName;
        this.rSocketRequester = rSocketRequester;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method invocationMethod = invocation.getMethod();
        AnnotationAttributes annotationAttributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
                invocationMethod, HttpExchange.class, true, true);

        HttpExchangeProperties httpExchange = new HttpExchangeProperties();
        httpExchange.setServiceName(serviceName);
        httpExchange.setUrl(annotationAttributes.getString("url"));
        httpExchange.setMethod(annotationAttributes.getString("method"));
        httpExchange.setContentType(annotationAttributes.getString("contentType"));
        httpExchange.setAccept(annotationAttributes.getStringArray("accept"));
        httpExchange.setHeaders(annotationAttributes.getStringArray("headers"));

        HttpDownstreamContext httpDownstreamContext = new HttpDownstreamContext();
        httpDownstreamContext.setHttpExchange(httpExchange);

        CarriesMetadata carriesMetadata = new CarriesMetadata();
        carriesMetadata.setProtocolContext(Map.of(HttpDownstreamContext.CarriesMetadata_KEY, httpDownstreamContext));

        Object[] arguments = invocation.getArguments();
        String data = "{}";
        if(arguments.length ==1) {
            data = objectMapper.writeValueAsString(arguments[0]);
        }

        Type genericReturnType = invocationMethod.getGenericReturnType();
        ParameterizedType pt = (ParameterizedType) genericReturnType;
        Type actualTypeArgument = pt.getActualTypeArguments()[0];
        Class<?> payloadType = (Class<?>) actualTypeArgument;

        Class<?> returnType = invocationMethod.getReturnType();
        Mono<?> resp = rSocketRequester.route("message.http.proxy.requestResponse")
                .metadata(metadataSpec->{
                    metadataSpec.metadata(carriesMetadata, MimeTypeUtils.APPLICATION_JSON);
                })
                .data(data)
                .retrieveMono(payloadType);
        return resp;
    }
}
