package io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory.interceptor;

public interface MessageInterceptor {

  void interceptor(MessageExchange exchange, MessageInterceptorChain chain);
}
