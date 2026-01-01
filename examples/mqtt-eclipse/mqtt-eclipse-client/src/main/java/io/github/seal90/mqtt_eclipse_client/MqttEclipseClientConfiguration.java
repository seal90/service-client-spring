package io.github.seal90.mqtt_eclipse_client;

import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.github.seal90.serviceclient.mqtt.eclipse.ProtocolTypeMqttEclipseChannelProperties;
import io.github.seal90.serviceclient.mqtt.eclipse.ProtocolTypeMqttEclipseProperties;
import io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory.interceptor.MessageExchange;
import io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory.interceptor.MessageInterceptor;
import io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory.interceptor.MessageInterceptorChain;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mqtt.core.Mqttv5ClientManager;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.nio.charset.StandardCharsets;

import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class MqttEclipseClientConfiguration {

  @Bean
  @ServiceClientInterceptor
  public MessageInterceptor messageInterceptor() {
    return (MessageExchange exchange, MessageInterceptorChain chain) -> {
      String serviceName = exchange.getAttribute(SERVICE_NAME);
      String channelName = exchange.getAttribute(CHANNEL_NAME);
      log.info("--- client interceptor serverName: {}, channelName: {}", serviceName, channelName);

      exchange.putHeader("CLIENT_TO_SERVER_HEADER_KEY", "CLIENT_TO_SERVER_HEADER_VALUE");
      exchange.putHeader("overlay-ns", "test");
      chain.doChain(exchange);
    };
  }

  @Configuration
  @EnableIntegration
  public static class MqttServiceClientReceiverConfig {

    @Autowired
    private ProtocolTypeMqttEclipseProperties properties;

    @Bean
    public MessageChannel serviceClientMqttInputChannel() {
      return new DirectChannel();
    }

    @Bean
    public Mqttv5PahoMessageDrivenChannelAdapter serviceClientMqttv5PahoMessageDrivenChannelAdapter() {
      ProtocolTypeMqttEclipseChannelProperties channelProperties = properties.configByServiceName("mqtt-moquette-server");

      MqttConnectionOptions options = new MqttConnectionOptions();
      options.setServerURIs(channelProperties.getServerURIs());
      options.setKeepAliveInterval(30);

      if (!channelProperties.getUserName().isEmpty()) {
        options.setUserName(channelProperties.getUserName());
        options.setPassword(channelProperties.getPassword().getBytes(StandardCharsets.UTF_8));
      }

      Mqttv5ClientManager mqttv5ClientManager = new Mqttv5ClientManager(options,
          channelProperties.getClientId()+"serviceclientreceive");
      mqttv5ClientManager.start();

      Mqttv5PahoMessageDrivenChannelAdapter adapter =
          new Mqttv5PahoMessageDrivenChannelAdapter(mqttv5ClientManager, channelProperties.getDefaultTopic());
      adapter.setCompletionTimeout(5000);
      adapter.setQos(1);
      adapter.setOutputChannel(serviceClientMqttInputChannel());
      return adapter;
    }

    @ServiceActivator(inputChannel = "serviceClientMqttInputChannel")
    public void serviceClientHandleMessage(Message<HelloData> message) {
      String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
      HelloData helloData = message.getPayload();

      System.out.println("ServiceClient pair receive MQTT message:"
          + "\n  Topic: " + topic
          + "\n  Payload: " + helloData
          + "\n  QoS: " + message.getHeaders().get("mqtt_receivedQos"));
    }
  }

  @Configuration
  @EnableIntegration
  public static class MqttSpringPairConfig {

    @Autowired
    private ProtocolTypeMqttEclipseProperties properties;

    // ------------------ send ----------------------------

    @Bean
    public MessageChannel mqttOutboundMessageChannel() {
      return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundMessageChannel")
    public MessageHandler mqttOutboundMessageHandler() {
      ProtocolTypeMqttEclipseChannelProperties channelProperties = properties.configByServiceName("mqtt-moquette-server");

      MqttConnectionOptions options = new MqttConnectionOptions();
      options.setServerURIs(channelProperties.getServerURIs());
      options.setKeepAliveInterval(30);

      if (!channelProperties.getUserName().isEmpty()) {
        options.setUserName(channelProperties.getUserName());
        options.setPassword(channelProperties.getPassword().getBytes(StandardCharsets.UTF_8));
      }

      Mqttv5PahoMessageHandler messageHandler = new Mqttv5PahoMessageHandler(options, channelProperties.getClientId());
      messageHandler.setAsync(true);
      messageHandler.setDefaultTopic("spring-default");
      return messageHandler;
    }

    // ------------------ receive ----------------------------

    @Bean
    public MessageChannel mqttInputChannel() {
      return new DirectChannel();
    }

    @Bean
    public Mqttv5PahoMessageDrivenChannelAdapter messageDrivenChannelAdapter() {
      ProtocolTypeMqttEclipseChannelProperties channelProperties = properties.configByServiceName("mqtt-moquette-server");

      MqttConnectionOptions options = new MqttConnectionOptions();
      options.setServerURIs(channelProperties.getServerURIs());
      options.setKeepAliveInterval(30);

      if (!channelProperties.getUserName().isEmpty()) {
        options.setUserName(channelProperties.getUserName());
        options.setPassword(channelProperties.getPassword().getBytes(StandardCharsets.UTF_8));
      }

      Mqttv5ClientManager mqttv5ClientManager = new Mqttv5ClientManager(options, channelProperties.getClientId()+"1");
      mqttv5ClientManager.start();

      Mqttv5PahoMessageDrivenChannelAdapter adapter =
          new Mqttv5PahoMessageDrivenChannelAdapter(mqttv5ClientManager, "spring-default");
      adapter.setCompletionTimeout(5000);
      adapter.setQos(1);
      adapter.setOutputChannel(mqttInputChannel());
      return adapter;
    }

//    @ServiceActivator(inputChannel = "mqttInputChannel")
//    public void handleMessage(Message<HelloData> message) {
//      String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
//      HelloData helloData = message.getPayload();
//
//      System.out.println("Spring pair receive MQTT message:"
//          + "\n  Topic: " + topic
//          + "\n  Payload: " + helloData
//          + "\n  QoS: " + message.getHeaders().get("mqtt_receivedQos"));
//    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
      String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
      byte[] payload = (byte[]) message.getPayload();
      String content = new String(payload, StandardCharsets.UTF_8);

      System.out.println("Spring pair receive MQTT message:"
          + "\n  Topic: " + topic
          + "\n  Payload: " + content
          + "\n  QoS: " + message.getHeaders().get("mqtt_receivedQos"));
    }
  }

}
