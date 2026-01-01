package io.github.seal90.serviceclient.grpc.protocoltypefactory;

import io.github.seal90.serviceclient.core.ChannelNamePrefix;
import io.github.seal90.serviceclient.core.ProtocolType;
import io.github.seal90.serviceclient.core.ProtocolTypeFactory;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.github.seal90.serviceclient.core.ServiceClientInterceptor;
import io.github.seal90.serviceclient.core.util.ApplicationContextBeanLookupUtils;
import io.github.seal90.serviceclient.core.util.MD5Util;
import io.github.seal90.serviceclient.grpc.protocoltypefactory.grpc.extension.properties.ProtocolTypeGrpcProperties;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.core.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

public class ProtocolTypeGrpcFactory implements ProtocolTypeFactory, ApplicationContextAware, EnvironmentAware, InitializingBean {

  public static final CallOptions.Key<String> SERVICE_NAME_KEY = CallOptions.Key.create(SERVICE_NAME);

  public static final CallOptions.Key<String> CHANNEL_NAME_KEY = CallOptions.Key.create(CHANNEL_NAME);

  private final ConcurrentHashMap<String, Channel> cache = new ConcurrentHashMap<>();

  private ApplicationContext applicationContext;

  private Environment environment;

  private ProtocolTypeGrpcProperties grpcProperties;

  @Override
  public <T> T create(Member injectionTarget, Class<T> injectionType, ServiceClient annotation) {
    final String serviceName = annotation.serviceName();
    final String channelName = annotation.channelName();
    final String[] interceptors = annotation.interceptors();

    if (Channel.class.equals(injectionType)) {
      return injectionType.cast(handleChannel(serviceName, channelName, interceptors));
    } else if (AbstractStub.class.isAssignableFrom(injectionType)) {
      return handleAbstractStub(serviceName, channelName, interceptors, injectionType);
    } else {
      if (injectionTarget != null) {
        throw new InvalidPropertyException(injectionTarget.getDeclaringClass(), injectionTarget.getName(),
            "Unsupported type " + injectionType.getName());
      } else {
        throw new BeanInstantiationException(injectionType, "Unsupported grpc stub or channel type");
      }
    }
  }

  public Channel handleChannel(String serviceName, String channelName, String[] interceptors) {

    String cacheKeyContent = serviceName + ":" + channelName + ":" + String.join(":", interceptors);
    String cacheKey = MD5Util.md5Hash(cacheKeyContent);

    return cache.computeIfAbsent(cacheKey, k -> {
      if(!channelName.isEmpty()) {
        if (ChannelNamePrefix.isContext(channelName)) {
          if (interceptors.length > 0) {
            throw new IllegalArgumentException("interceptors are not allowed for channel name 'context'");
          }
          return (Channel)applicationContext.getBean(ChannelNamePrefix.extractContext(channelName));
        }
      }

      String address = findAddress(serviceName, channelName);
      List<ClientInterceptor> clientInterceptors = buildClientInterceptors(serviceName, channelName, interceptors);
      // TODO config by properties
      ManagedChannel channel = ManagedChannelBuilder
          .forTarget(address)
          .defaultLoadBalancingPolicy("round_robin")
          .intercept(clientInterceptors)
          .usePlaintext()
          .build();

      return channel;
    });
  }

  private String findAddress(String serviceName, String channelName) {
    String address;
    if(!channelName.isEmpty()) {
      if (ChannelNamePrefix.isStatic(channelName)) {
        address = ChannelNamePrefix.extractStatic(channelName);
        address = environment.resolveRequiredPlaceholders(address);
      } else if (ChannelNamePrefix.isChannel(channelName)) {
        address = grpcProperties.addressByChannel(ChannelNamePrefix.extractChannel(channelName));
      } else if (ChannelNamePrefix.isDefault(channelName)) {
        address = grpcProperties.addressByDefault();
      } else if (ChannelNamePrefix.isLb(channelName)) {
        address = channelName;
      } else {
        // default is channel name
        address = grpcProperties.addressByChannel(channelName);
      }
    } else {
      address = grpcProperties.addressByServiceName(serviceName);
      if(address == null) {
        address = ChannelNamePrefix.withLb(serviceName);
      }
    }
    return address;
  }

  public <T> T handleAbstractStub(String serviceName, String channelName, String[] interceptors, Class<T> injectionType) {
    Channel channel = handleChannel(serviceName, channelName, interceptors);
    // TODO cache?
    @SuppressWarnings("unchecked")
    AbstractStub<?> stub = createStub((Class<? extends AbstractStub>)injectionType, channel);
    return injectionType.cast(stub);
  }

  private <T extends AbstractStub<T>> T createStub(final Class<T> stubClass, final Channel channel) {
    try {
      // Search for public static *Grpc#new*Stub(Channel)
      final Class<?> declaringClass = stubClass.getDeclaringClass();
      if (declaringClass != null) {
        for (final Method method : declaringClass.getMethods()) {
          final String name = method.getName();
          final int modifiers = method.getModifiers();
          final Parameter[] parameters = method.getParameters();
          if (name.startsWith("new") && name.endsWith("Stub")
              && Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)
              && method.getReturnType().isAssignableFrom(stubClass)
              && parameters.length == 1
              && Channel.class.equals(parameters[0].getType())) {
            return stubClass.cast(method.invoke(null, channel));
          }
        }
      }

      // Search for a public constructor *Stub(Channel)
      final Constructor<T> constructor = stubClass.getConstructor(Channel.class);
      return constructor.newInstance(channel);
    } catch (final Exception e) {
      throw new BeanInstantiationException(stubClass, "Failed to create gRPC client", e);
    }
  }

  private List<ClientInterceptor> buildClientInterceptors(String serviceName, String channelName, String[] interceptors) {
    List<ClientInterceptor> interceptorBeans = new ArrayList<>(interceptors.length);
    for(String interceptor : interceptors) {
      interceptorBeans.add(applicationContext.getBean(interceptor, ClientInterceptor.class));
    }

    List<ClientInterceptor> globalClientInterceptor = ApplicationContextBeanLookupUtils
        .getBeansWithAnnotation(applicationContext, ClientInterceptor.class, ServiceClientInterceptor.class);
    interceptorBeans.addAll(globalClientInterceptor);

    AnnotationAwareOrderComparator.sort(interceptorBeans);
    interceptorBeans.add(new ClientInterceptor() {
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

  @Override
  public String supportProtocol() {
    return ProtocolType.GRPC;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    grpcProperties = this.applicationContext.getBean(ProtocolTypeGrpcProperties.class);
  }

}