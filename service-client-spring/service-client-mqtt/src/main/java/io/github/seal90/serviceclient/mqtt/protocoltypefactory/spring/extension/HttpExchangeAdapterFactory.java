package io.github.seal90.serviceclient.mqtt.protocoltypefactory.spring.extension;

import org.springframework.web.service.invoker.HttpExchangeAdapter;

public interface HttpExchangeAdapterFactory {

    HttpExchangeAdapter create(String serviceName, String channelName, String[] interceptors);
}