package io.github.seal90.serviceclient.carries.http;

import io.github.seal90.serviceclient.carries.http.extension.CarriesHttpMethodInterceptor;
import io.github.seal90.serviceclient.carries.http.properties.CarriesHttpByRSocketProperties;
import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.core.ServiceClient;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.messaging.rsocket.RSocketRequester;

import java.lang.reflect.Member;

public class CarriesHttpProtocolTypeFactory implements ProtocolTypeFactory {

    private ApplicationContext applicationContext;
    private Environment environment;
    private CarriesHttpByRSocketProperties carriesHttpByRSocketProperties;
    RSocketRequester.Builder rsocketRequesterBuilder;

    public CarriesHttpProtocolTypeFactory(ApplicationContext applicationContext, Environment environment
            , CarriesHttpByRSocketProperties carriesHttpByRSocketProperties, RSocketRequester.Builder rsocketRequesterBuilder) {
        this.applicationContext = applicationContext;
        this.environment = environment;
        this.carriesHttpByRSocketProperties = carriesHttpByRSocketProperties;
        this.rsocketRequesterBuilder = rsocketRequesterBuilder;
    }


    @Override
    public <T> T create(Member injectionTarget, Class<T> injectionType, ServiceClient annotation) {
        // TODO for more parse config
        String serviceName = annotation.serviceName();
//        RSocketRequester.Builder builder = applicationContext.getBean(RSocketRequester.Builder.class);
        String address = carriesHttpByRSocketProperties.addressByServiceName(serviceName);
        String[] addr = address.split(":");
        RSocketRequester rSocketRequester = rsocketRequesterBuilder.tcp(addr[0], Integer.parseInt(addr[1]));
        // TODO for factory
        return (T)ProxyFactory.getProxy(injectionType, new CarriesHttpMethodInterceptor(serviceName, rSocketRequester));
    }

    @Override
    public String supportProtocol() {
        return "CARRIES_HTTP";
    }
}
