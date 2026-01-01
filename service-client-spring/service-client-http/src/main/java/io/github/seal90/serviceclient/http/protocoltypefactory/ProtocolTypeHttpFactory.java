package io.github.seal90.serviceclient.http.protocoltypefactory;

import io.github.seal90.serviceclient.core.ProtocolType;
import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.github.seal90.serviceclient.core.util.MD5Util;
import io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.HttpExchangeAdapterFactory;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.lang.reflect.Member;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolTypeHttpFactory implements ProtocolTypeFactory {

  private final HttpExchangeAdapterFactory httpExchangeAdapterFactory;

  private final ConcurrentHashMap<String, HttpExchangeAdapter> cacheHttpExchangeAdapters = new ConcurrentHashMap<>();

  public ProtocolTypeHttpFactory(HttpExchangeAdapterFactory httpExchangeAdapterFactory) {
    this.httpExchangeAdapterFactory = httpExchangeAdapterFactory;
  }

  @Override
  public <T> T create(Member injectionTarget, Class<T> injectionType, ServiceClient annotation) {
    final String serviceName = annotation.serviceName();
    final String channelName = annotation.channelName();
    final String[] interceptors = annotation.interceptors();

    String cacheKeyContent = serviceName + ":" + channelName + ":" + String.join(":", interceptors);
    String cacheKey = MD5Util.md5Hash(cacheKeyContent);

    HttpExchangeAdapter adapter = cacheHttpExchangeAdapters.computeIfAbsent(cacheKey, k ->
        httpExchangeAdapterFactory.create(serviceName, channelName, interceptors));

    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
    return factory.createClient(injectionType);
  }

  @Override
  public String supportProtocol() {
    return ProtocolType.HTTP;
  }

}