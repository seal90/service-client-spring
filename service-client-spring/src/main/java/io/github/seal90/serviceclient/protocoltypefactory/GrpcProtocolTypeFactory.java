package io.github.seal90.serviceclient.protocoltypefactory;

import io.github.seal90.serviceclient.ServiceClient;
import io.github.seal90.serviceclient.ProtocolType;
import io.github.seal90.serviceclient.ProtocolTypeFactory;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.GrpcClientFactory;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

public class GrpcProtocolTypeFactory implements ProtocolTypeFactory, ApplicationContextAware {

  public static final CallOptions.Key<String> SERVICE_NAME_KEY = CallOptions.Key.create(SERVICE_NAME);

  public static final CallOptions.Key<String> CHANNEL_NAME_KEY = CallOptions.Key.create(CHANNEL_NAME);

  private ApplicationContext applicationContext;

  private GrpcClientFactory grpcClientFactory;

  private GrpcChannelFactory grpcChannelFactory;

  @Override
  public <T> T create(Member injectionTarget, Class<T> injectionType, ServiceClient annotation) {
    final String serviceName = annotation.serviceName();
    final String channelName = annotation.channelName();
    final String[] interceptors = annotation.interceptors();

    List<ClientInterceptor> interceptorBeans = buildClientInterceptors(interceptors, serviceName, channelName);

    String finalChannelName = channelName(serviceName, channelName);
    if (Channel.class.equals(injectionType)) {
      return handleChannel(finalChannelName, injectionType, interceptorBeans);
    }
    else if (AbstractStub.class.isAssignableFrom(injectionType)) {
      return handleAbstractStub(finalChannelName, injectionType, interceptorBeans);
    }
    else {
      if (injectionTarget != null) {
        throw new InvalidPropertyException(injectionTarget.getDeclaringClass(), injectionTarget.getName(),
            "Unsupported type " + injectionType.getName());
      }
      else {
        throw new BeanInstantiationException(injectionType, "Unsupported grpc stub or channel type");
      }
    }
  }

  private List<ClientInterceptor> buildClientInterceptors(String[] interceptors, String serviceName, String channelName) {
    List<ClientInterceptor> interceptorBeans = new ArrayList<>(interceptors.length);
    for(String interceptor : interceptors) {
      interceptorBeans.add(applicationContext.getBean(interceptor, ClientInterceptor.class));
    }

    AnnotationAwareOrderComparator.sort(interceptorBeans);
    interceptorBeans.addFirst(new ClientInterceptor() {
      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> method,
                                                                 final CallOptions callOptions, final Channel next) {
        CallOptions withNameCallOptions = callOptions.withOption(SERVICE_NAME_KEY, serviceName)
            .withOption(CHANNEL_NAME_KEY, channelName);
        return next.newCall(method, withNameCallOptions);
      }
    });
    return interceptorBeans;
  }

  /**
   * Handles the injection of Channel Type.
   * @param finalChannelName the resolved channel name
   * @param injectionType the expected channel class
   * @return the Channel
   * @param <T> The type of the value to be injected.
   */
  private <T> T handleChannel(String finalChannelName,
                              Class<T> injectionType, List<ClientInterceptor> interceptorBeans) {

    Channel channel = fetchContextBean(finalChannelName, Channel.class);
    // TODO use same channel GrpcClientFactory created?
    if(channel == null) {
      channel = grpcChannelFactory.createChannel(finalChannelName);
    }
    channel = ClientInterceptors.intercept(channel, interceptorBeans);
    return injectionType.cast(channel);
  }

  /**
   * Handles the injection of AbstractStub Type.
   * @param finalChannelName the resolved channel name
   * @param injectionType the expected channel class
   * @return the AbstractStub
   * @param <T> The type of the value to be injected.
   */
  private <T> T handleAbstractStub(String finalChannelName,
                                   Class<T> injectionType, List<ClientInterceptor> interceptorBeans) {
    AbstractStub<?> stub = fetchContextBean(finalChannelName, AbstractStub.class);
    if(stub == null) {
      stub = (AbstractStub<?>) grpcClientFactory.getClient(finalChannelName, (Class<? extends AbstractStub>) injectionType, null);
    }
    stub = stub.withInterceptors(interceptorBeans.toArray(new ClientInterceptor[]{}));
    return injectionType.cast(stub);
  }

  /**
   * Computes the channel name to use.
   * @param serviceName {@link ServiceClient} annotation serviceName.
   * @param channelName {@link ServiceClient} annotation channelName.
   * @return The channel name to use.
   */
  private String channelName(String serviceName, String channelName) {
    if (!channelName.isEmpty()) {
      return channelName;
    }
    if (!serviceName.isEmpty()) {
      return serviceName;
    }
    return "default";
  }

  private <T> T fetchContextBean(String name, Class<T> type) {
    if(applicationContext.containsBean(name)) {
      Object bean = applicationContext.getBean(name);
      if(type.isAssignableFrom(bean.getClass())) {
        return type.cast(bean);
      }
    }
    return null;
  }

  @Override
  public String supportProtocol() {
    return ProtocolType.GRPC;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
    this.grpcClientFactory = new GrpcClientFactory(applicationContext);
    this.grpcChannelFactory = applicationContext.getBean(GrpcChannelFactory.class);
  }
}