package io.github.seal90.serviceclient.protocoltypefactory;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.seal90.serviceclient.ServiceClient;
import io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor;
import io.github.seal90.serviceclient.ProtocolType;
import io.github.seal90.serviceclient.ProtocolTypeFactory;
import io.github.seal90.serviceclient.protocoltypefactory.feign.extension.FeignClientFactoryBeanExtension;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

public class FeignProtocolTypeFactory implements ProtocolTypeFactory, ApplicationContextAware {

  private ApplicationContext applicationContext;

  @Override
  public <T> T create(Member injectionTarget, Class<T> injectionType, ServiceClient annotation) {
    final String serviceName = annotation.serviceName();
    final String channelName = annotation.channelName();
    final String[] interceptors = annotation.interceptors();

    List<RequestInterceptor> interceptorBeans = buildRequestInterceptors(interceptors, serviceName, channelName);
    interceptorBeans.add(new NameAddRequestInterceptor(serviceName, channelName));
    interceptorBeans.add(new NameRemoveRequestInterceptor());

    String finalChannelName = channelName(serviceName, channelName);

    // TODO Optimize the implementation here
    FeignClientFactoryBeanExtension factoryBean = new FeignClientFactoryBeanExtension();
    factoryBean.setName(finalChannelName);
    factoryBean.setContextId(finalChannelName);
    factoryBean.setType(injectionType);
    factoryBean.setRequestInterceptors(interceptorBeans);
    factoryBean.setApplicationContext(applicationContext);
//    Object obj = registerSingletonBean(finalChannelName, factoryBean);
    Object obj = factoryBean.getObject();

    return injectionType.cast(obj);
  }

  public Object registerSingletonBean(String beanName,Object singletonObject){
    ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
    DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getAutowireCapableBeanFactory();
    defaultListableBeanFactory.registerSingleton(beanName,singletonObject);

    return configurableApplicationContext.getBean(beanName);
  }

  private List<RequestInterceptor> buildRequestInterceptors(String[] interceptors, String serviceName, String channelName) {
    List<RequestInterceptor> interceptorBeans = new ArrayList<>(interceptors.length);
    for(String interceptor : interceptors) {
      interceptorBeans.add(applicationContext.getBean(interceptor, RequestInterceptor.class));
    }
    return interceptorBeans;
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

  @Override
  public String supportProtocol() {
    return ProtocolType.HTTP_FEIGN;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  private static class NameAddRequestInterceptor implements RequestInterceptor, Ordered {

    private final String serviceName;
    private final String channelName;

    public NameAddRequestInterceptor(String serviceName, String channelName) {
      this.serviceName = serviceName;
      this.channelName = channelName;
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
      requestTemplate.header(ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME, serviceName);
      requestTemplate.header(ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME, channelName);
    }

    @Override
    public int getOrder() {
      return HIGHEST_PRECEDENCE;
    }
  }

  private static class NameRemoveRequestInterceptor implements RequestInterceptor, Ordered {

    @Override
    public void apply(RequestTemplate requestTemplate) {
      requestTemplate.removeHeader(ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME);
      requestTemplate.removeHeader(ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME);
    }

    @Override
    public int getOrder() {
      return LOWEST_PRECEDENCE;
    }
  }
}
