package io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory.interceptor;

import lombok.Getter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class MessageExchange {

  private final Map<String, Object> attributes;

  private final Message<?> message;

  public MessageExchange(Message<?> message) {
    this.message = message;
    this.attributes = new ConcurrentHashMap<>();
  }

  public MessageHeaders getHeaders() {
    return this.message.getHeaders();
  }

  public void putHeader(String key, Object val) {
    this.message.getHeaders().put(key, val);
  }

  public <T> T getAttribute(String name) {
    return (T)this.attributes.get(name);
  }

  public void putAttribute(String key, Object val) {
    this.attributes.put(key, val);
  }

}
