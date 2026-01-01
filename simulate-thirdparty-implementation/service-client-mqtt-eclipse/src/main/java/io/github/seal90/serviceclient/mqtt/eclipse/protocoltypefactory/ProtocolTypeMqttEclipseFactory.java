package io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seal90.serviceclient.core.ChannelNamePrefix;
import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.github.seal90.serviceclient.core.util.ApplicationContextBeanLookupUtils;
import io.github.seal90.serviceclient.core.util.MD5Util;
import io.github.seal90.serviceclient.mqtt.eclipse.ProtocolTypeMqttEclipse;
import io.github.seal90.serviceclient.mqtt.eclipse.ProtocolTypeMqttEclipseChannelProperties;
import io.github.seal90.serviceclient.mqtt.eclipse.ProtocolTypeMqttEclipseProperties;
import io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory.interceptor.MessageExchange;
import io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory.interceptor.MessageInterceptor;
import io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory.interceptor.MessageInterceptorChain;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.integration.mqtt.core.Mqttv5ClientManager;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaderMapper;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeTypeUtils;

import java.lang.reflect.Member;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

@Slf4j
public class ProtocolTypeMqttEclipseFactory implements ProtocolTypeFactory, ApplicationContextAware, EnvironmentAware {

  private ApplicationContext applicationContext;

  private Environment environment;

  private final ConcurrentHashMap<String, MessageChannel> cache = new ConcurrentHashMap<>();

  @Override
  public <T> T create(Member injectionTarget, Class<T> injectionType, ServiceClient annotation) {
    final String serviceName = annotation.serviceName();
    final String channelName = annotation.channelName();
    final String[] interceptors = annotation.interceptors();

    String cacheKeyContent = serviceName + ":" + channelName + ":" + String.join(":", interceptors);
    String cacheKey = MD5Util.md5Hash(cacheKeyContent);

    MessageChannel channel = cache.computeIfAbsent(cacheKey, k -> {
      if(!channelName.isEmpty()) {
        if(ChannelNamePrefix.isContext(channelName)) {
          if (interceptors.length > 0) {
            throw new IllegalArgumentException("interceptors are not allowed for channel name 'context'");
          }
          return (MessageChannel)applicationContext.getBean(ChannelNamePrefix.extractContext(channelName));
        }
      }

      List<MessageInterceptor> allInterceptors = buildInterceptors(serviceName, channelName, interceptors);
      MessageInterceptorChain chain = new MessageInterceptorChain(allInterceptors);
      return new ProxyMessageChannel(chain);
    });

    return injectionType.cast(channel);
  }

  public ProtocolTypeMqttEclipseChannelProperties findConfig(String serviceName, String channelName) {
    ProtocolTypeMqttEclipseProperties properties = applicationContext.getBean(ProtocolTypeMqttEclipseProperties.class);
    ProtocolTypeMqttEclipseChannelProperties channelProperties = null;
    if(!channelName.isEmpty()) {
      if (ChannelNamePrefix.isStatic(channelName)) {
        String address = ChannelNamePrefix.extractStatic(channelName);
        address = environment.resolveRequiredPlaceholders(address);
        channelProperties = new ProtocolTypeMqttEclipseChannelProperties();
        channelProperties.setServerURIs(new String[]{address});
      } else if (ChannelNamePrefix.isChannel(channelName)) {
        channelProperties = properties.configByChannel(ChannelNamePrefix.extractChannel(channelName));
      } else if (ChannelNamePrefix.isDefault(channelName)) {
        channelProperties = properties.configByDefault();
      } else if (ChannelNamePrefix.isLb(channelName)) {
        // Does not support dynamic discovery scenarios.
      } else {
        // default is channel name
        channelProperties = properties.configByChannel(channelName);
      }
    } else {
      channelProperties = properties.configByServiceName(serviceName);
      if(channelProperties == null) {
        // Does not support dynamic discovery scenarios.
      }
    }
    return channelProperties;
  }

  public List<MessageInterceptor> buildInterceptors(String serviceName, String channelName, String[] interceptors) {
    ProtocolTypeMqttEclipseChannelProperties channelProperties = findConfig(serviceName, channelName);
    if(channelProperties == null) {
      throw new BeanCreationException("Channel configuration not found.");
    }
    ServiceNameAddMessageInterceptor serviceNameAddMessageInterceptor = new ServiceNameAddMessageInterceptor(serviceName, channelName);
    ExecMessageInterceptor execMessageInterceptor = new ExecMessageInterceptor(channelProperties);

    List<MessageInterceptor> allInterceptors = new ArrayList<>();

    for(String interceptor : interceptors) {
      MessageInterceptor messageInterceptor = (MessageInterceptor)applicationContext.getBean(interceptor);
      allInterceptors.add(messageInterceptor);
    }
    List<MessageInterceptor> globalInterceptors = ApplicationContextBeanLookupUtils
        .getBeansWithAnnotation(applicationContext, MessageInterceptor.class, ServiceClientInterceptor.class);
    allInterceptors.addAll(globalInterceptors);

    AnnotationAwareOrderComparator.sort(allInterceptors);

    allInterceptors.addFirst(serviceNameAddMessageInterceptor);
    allInterceptors.add(execMessageInterceptor);

    return allInterceptors;
  }

  private static class ServiceNameAddMessageInterceptor implements MessageInterceptor {

    private final String serviceName;
    private final String channelName;

    public ServiceNameAddMessageInterceptor(String serviceName, String channelName) {
      this.serviceName = serviceName;
      this.channelName = channelName;
    }

    @Override
    public void interceptor(MessageExchange exchange, MessageInterceptorChain chain) {
      exchange.putAttribute(SERVICE_NAME, serviceName);
      exchange.putAttribute(CHANNEL_NAME, channelName);
      chain.doChain(exchange);
    }
  }

  private static class ExecMessageInterceptor implements MessageInterceptor {

    // TODO close before destroy
    private Mqttv5PahoMessageHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExecMessageInterceptor(ProtocolTypeMqttEclipseChannelProperties properties) {
      init(properties);
    }

    private void init(ProtocolTypeMqttEclipseChannelProperties properties) {

      MqttConnectionOptions options = new MqttConnectionOptions();
      options.setServerURIs(properties.getServerURIs());
      options.setCleanStart(true);
      options.setSessionExpiryInterval(60L);
      options.setKeepAliveInterval(30);

      options.setUserName(properties.getUserName());
      String password = properties.getPassword();
      if(password != null) {
        options.setPassword(password.getBytes(StandardCharsets.UTF_8));
      }

      Mqttv5ClientManager mqttv5ClientManager = new Mqttv5ClientManager(options, properties.getClientId());
      mqttv5ClientManager.start();

      MqttHeaderMapper mqttHeaderMapper = new MqttHeaderMapper();
      String[] outboundHeaderNames = {
          MessageHeaders.CONTENT_TYPE,
          MqttHeaders.MESSAGE_EXPIRY_INTERVAL,
          MqttHeaders.RESPONSE_TOPIC,
          MqttHeaders.CORRELATION_DATA,
          "hello"
      };
      mqttHeaderMapper.setOutboundHeaderNames(outboundHeaderNames);

      Mqttv5PahoMessageHandler handler = new Mqttv5PahoMessageHandler(mqttv5ClientManager);
      handler.setDefaultTopic(properties.getDefaultTopic());
      handler.setHeaderMapper(mqttHeaderMapper);
      handler.start();
      this.handler = handler;
    }

    @Override
    public void interceptor(MessageExchange exchange, MessageInterceptorChain chain) {
      Message<?> message = exchange.getMessage();
      Object payload = message.getPayload();
      MessageHeaders headers = message.getHeaders();
      byte[] data;
      String contentType = null;
      if(payload.getClass() == byte[].class) {
        data = (byte[])payload;
      } else if(payload.getClass() == String.class) {
        data = ((String)payload).getBytes(StandardCharsets.UTF_8);
        contentType = MimeTypeUtils.TEXT_PLAIN_VALUE;
      } else {
        try {
          data = objectMapper.writeValueAsString(payload).getBytes();
          contentType = MimeTypeUtils.APPLICATION_JSON_VALUE;
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }
      if(contentType != null) {
        headers.put(MessageHeaders.CONTENT_TYPE, contentType);
      }
      Message<?> newMessage = MessageBuilder.withPayload(data).copyHeaders(headers).build();
      handler.handleMessage(newMessage);
    }
  }

  private static class ProxyMessageChannel implements MessageChannel {

    private final MessageInterceptorChain chain;

    public ProxyMessageChannel(MessageInterceptorChain chain) {
      this.chain = chain;
    }

    @Override
    public boolean send(Message<?> message, long timeout) {

      MutableMessage<?> mutableMessage = new MutableMessage<>(message.getPayload(), message.getHeaders());
      MessageExchange exchange = new MessageExchange(mutableMessage);
      boolean success = true;
      try {
        chain.doChain(exchange);
      } catch (Exception ex) {
        log.warn("send message error.", ex);
        success = false;
      }
      return success;
    }
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public String supportProtocol() {
    return ProtocolTypeMqttEclipse.MQTT_ECLIPSE;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}