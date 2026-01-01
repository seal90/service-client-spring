package io.github.seal90.serviceclient.protocoltypefactory.feign.extension;

import feign.Feign;
import feign.RequestInterceptor;
import io.github.seal90.serviceclient.util.ApplicationContextBeanLookupUtils;
import lombok.Setter;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;

@Setter
public class FeignClientFactoryBeanExtension extends FeignClientFactoryBean {

  private List<RequestInterceptor> requestInterceptors;

  @Override
  protected void configureUsingConfiguration(FeignClientFactory context, Feign.Builder builder) {
    super.configureUsingConfiguration(context, builder);
    List<RequestInterceptor> requestInterceptors = findGlobalRequestInterceptor();

    List<RequestInterceptor> interceptors = new ArrayList<>();
    if (requestInterceptors != null) {
      interceptors.addAll(requestInterceptors);
    }
    if (this.requestInterceptors != null) {
      interceptors.addAll(this.requestInterceptors);
    }
    AnnotationAwareOrderComparator.sort(interceptors);
    builder.requestInterceptors(interceptors);
  }

  private List<RequestInterceptor> findGlobalRequestInterceptor() {
    return ApplicationContextBeanLookupUtils.getBeansWithAnnotation(super.getApplicationContext(),
        RequestInterceptor.class, GlobalRequestInterceptor.class);
  }
}
