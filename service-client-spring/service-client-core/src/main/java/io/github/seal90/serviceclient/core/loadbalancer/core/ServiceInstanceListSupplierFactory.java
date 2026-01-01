package io.github.seal90.serviceclient.core.loadbalancer.core;

import io.github.seal90.serviceclient.core.loadbalancer.ServiceInstanceListSupplier;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.concurrent.ConcurrentHashMap;

public class ServiceInstanceListSupplierFactory implements ApplicationContextAware {

  private ApplicationContext applicationContext;

  private ConcurrentHashMap<String, ServiceInstanceListSupplier> suppliers = new ConcurrentHashMap<>();


//  public ServiceInstanceListSupplier manufacture(String serviceName) {
//    applicationContext.getBeanProvider(ServiceInstanceListSupplier.class).
//
//  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

}
