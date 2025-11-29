package io.github.seal90.serviceclient.protocoltypefactory;

import io.github.seal90.serviceclient.ServiceClient;
import io.github.seal90.serviceclient.ProtocolType;
import io.github.seal90.serviceclient.ProtocolTypeFactory;
import io.github.seal90.serviceclient.protocoltypefactory.spring.extension.HttpExchangeAdapterFactory;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.lang.reflect.Member;

public class HttpProtocolTypeFactory implements ProtocolTypeFactory {

  private final HttpExchangeAdapterFactory httpExchangeAdapterFactory;

  public HttpProtocolTypeFactory(HttpExchangeAdapterFactory httpExchangeAdapterFactory) {
    this.httpExchangeAdapterFactory = httpExchangeAdapterFactory;
  }

  @Override
  public <T> T create(Member injectionTarget, Class<T> injectionType, ServiceClient annotation) {
    final String serviceName = annotation.serviceName();
    final String channelName = annotation.channelName();
    final String[] interceptors = annotation.interceptors();

    HttpExchangeAdapter adapter = httpExchangeAdapterFactory.create(serviceName, channelName, interceptors);
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
    return factory.createClient(injectionType);
  }

  @Override
  public String supportProtocol() {
    return ProtocolType.HTTP;
  }

}