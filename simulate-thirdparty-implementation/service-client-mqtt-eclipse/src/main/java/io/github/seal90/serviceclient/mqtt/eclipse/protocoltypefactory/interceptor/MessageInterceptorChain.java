package io.github.seal90.serviceclient.mqtt.eclipse.protocoltypefactory.interceptor;

import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class MessageInterceptorChain {

  private final List<MessageInterceptor> interceptors;

  private final MessageInterceptor currentInterceptor;

  private final MessageInterceptorChain chain;

  public MessageInterceptorChain(List<MessageInterceptor> interceptors) {
    this.interceptors = Collections.unmodifiableList(interceptors);
    MessageInterceptorChain chain = initChain(interceptors);
    this.currentInterceptor = chain.currentInterceptor;
    this.chain = chain;
  }

  private MessageInterceptorChain(List<MessageInterceptor> interceptors, @Nullable MessageInterceptor currentInterceptor, @Nullable MessageInterceptorChain chain) {
    this.interceptors = interceptors;
    this.currentInterceptor = currentInterceptor;
    this.chain = chain;
  }

  private static MessageInterceptorChain initChain(List<MessageInterceptor> interceptors) {
    MessageInterceptorChain chain = new MessageInterceptorChain(interceptors, null, null);

    for(ListIterator<MessageInterceptor> iterator = interceptors.listIterator(interceptors.size());
        iterator.hasPrevious();
        chain = new MessageInterceptorChain(interceptors, iterator.previous(), chain)) {
    }

    return chain;
  }

  public void doChain(MessageExchange exchange) {
    if(this.currentInterceptor != null && this.chain != null) {
      currentInterceptor.interceptor(exchange, this.chain);
    }
  }

}
