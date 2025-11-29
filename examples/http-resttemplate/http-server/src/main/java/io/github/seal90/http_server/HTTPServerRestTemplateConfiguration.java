package io.github.seal90.http_server;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class HTTPServerRestTemplateConfiguration {

  @Bean
  public WebMvcConfigurer headerDealWebMvcConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
          @Override
          public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String value = request.getHeader("CLIENT_TO_SERVER_HEADER_KEY");
            log.info("--- server receive client header CLIENT_TO_SERVER_HEADER_KEY : {}", value);
            String overlyNS = request.getHeader("overlay-ns");
            log.info("--- server receive client header overlay-ns : {}", overlyNS);

            response.addHeader("SERVER_TO_CLIENT_HEADER_KEY", "SERVER_TO_CLIENT_HEADER_VALUE");
            return true;
          }

          @Override
          public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
            // not work
//            response.addHeader("SERVER_TO_CLIENT_HEADER_KEY", "SERVER_TO_CLIENT_HEADER_VALUE");
          }

          @Override
          public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

          }
        });
      }
    };
  }

}
