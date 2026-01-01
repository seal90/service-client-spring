package io.github.seal90.serviceclient.mqtt.protocoltypefactory;

import io.github.seal90.serviceclient.core.ProtocolType;
import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.core.ServiceClient;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.messaging.MessageChannel;

import java.lang.reflect.Member;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolTypeMqttFactory implements ProtocolTypeFactory {

  private final ConcurrentHashMap<String, MessageChannel> messageChannels = new ConcurrentHashMap<>();

  @Override
  public <T> T create(Member injectionTarget, Class<T> injectionType, ServiceClient annotation) {
    final String serviceName = annotation.serviceName();
    final String channelName = annotation.channelName();
    final String[] interceptors = annotation.interceptors();

    MqttConnectOptions options = new MqttConnectOptions();

    DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
    factory.setConnectionOptions(options);
//    String cacheKeyContent = serviceName + ":" + channelName + ":" + String.join(":", interceptors);
//    String cacheKey = MD5Util.md5Hash(cacheKeyContent);
//
//    MessageChannel messageChannel = messageChannels.computeIfAbsent(cacheKey, k ->
//        new DirectChannel());

    return injectionType.cast(new DirectChannel());
  }

  @Override
  public String supportProtocol() {
    return ProtocolType.HTTP;
  }

}