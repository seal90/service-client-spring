package io.github.seal90.serviceclient.protocoltypefactory.spring.extension.headerforwarding;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Data
@AllArgsConstructor
public class ForwardWebHeaderRequestInterceptor implements ClientHttpRequestInterceptor, Ordered {

  private String[] forwardWebHeaders;

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    if(forwardWebHeaders == null || forwardWebHeaders.length == 0) {
      return execution.execute(request, body);
    }

    HttpHeaders requestHeaders = request.getHeaders();
    RequestAttributes webRequestAttributes = RequestContextHolder.getRequestAttributes();
    if (webRequestAttributes instanceof ServletRequestAttributes attributes) {
      HttpServletRequest webRequest = attributes.getRequest();

      for(String forwardWebHeader: forwardWebHeaders) {
        List<String> requestHeaderValues = requestHeaders.get(forwardWebHeader);
        if(requestHeaderValues == null || requestHeaderValues.isEmpty()) {
          Enumeration<String> webHeaderValues = webRequest.getHeaders(forwardWebHeader);
          if(webHeaderValues != null) {
            requestHeaders.addAll(forwardWebHeader, Collections.list(webHeaderValues));
          }
        }
      }
    }

    return execution.execute(request, body);
  }

  @Override
  public int getOrder() {
    return 10;
  }
}
