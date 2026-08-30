package io.github.seal90.serviceclient.carries.by.rsocket;

import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.ExecMessageRSocket;
import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.MessageRSocket;
import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.MessageRSocketRequesterInterceptor;
import io.github.seal90.serviceclient.carries.by.rsocket.extension.CarriesByRSocketMethodInterceptor;
import io.github.seal90.serviceclient.carries.by.rsocket.properties.CarriesByRSocketProperties;
import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.rsocket.metadata.WellKnownMimeType;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeType;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CarriesByRSocketProtocolTypeFactory implements ProtocolTypeFactory {

    private ApplicationContext applicationContext;
    private Environment environment;
    private CarriesByRSocketProperties carriesByRSocketProperties;
    RSocketRequester.Builder rsocketRequesterBuilder;

    public CarriesByRSocketProtocolTypeFactory(ApplicationContext applicationContext, Environment environment
            , CarriesByRSocketProperties carriesByRSocketProperties, RSocketRequester.Builder rsocketRequesterBuilder) {
        this.applicationContext = applicationContext;
        this.environment = environment;
        this.carriesByRSocketProperties = carriesByRSocketProperties;
        this.rsocketRequesterBuilder = rsocketRequesterBuilder;
    }


    @Override
    public <T> T create(Member injectionTarget, Class<T> injectionType, ServiceClient annotation) {
        // TODO for more parse config
        String serviceName = annotation.serviceName();
//        RSocketRequester.Builder builder = applicationContext.getBean(RSocketRequester.Builder.class);
        String address = carriesByRSocketProperties.addressByServiceName(serviceName);
        String[] addr = address.split(":");
        RSocketRequester rSocketRequester = rsocketRequesterBuilder
                .dataMimeType(MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()))
                .tcp(addr[0], Integer.parseInt(addr[1]));

        MessageRSocket execMessageRSocket = new ExecMessageRSocket(rSocketRequester);
        Map<String, MessageRSocketRequesterInterceptor> interceptorMap = applicationContext.getBeansOfType(MessageRSocketRequesterInterceptor.class);
        List<MessageRSocketRequesterInterceptor> interceptors = new ArrayList<>(interceptorMap.values());
        AnnotationAwareOrderComparator.sort(interceptors);
        MessageRSocket messageRSocket = interceptors.stream()
                .reduce(
                        execMessageRSocket,
                        (current, interceptor) -> interceptor.apply(current),
                        (a, b) -> a);

        // TODO for factory
        return (T) ProxyFactory.getProxy(injectionType, new CarriesByRSocketMethodInterceptor(serviceName, rSocketRequester, messageRSocket));
    }

    @Override
    public String supportProtocol() {
        return "CARRIES_BY_RSOCKET";
    }
}
