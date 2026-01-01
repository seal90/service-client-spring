package io.github.seal90.serviceclient.protocoltypefactory.spring.extension.nameresovle;

import io.github.seal90.serviceclient.protocoltypefactory.spring.extension.properties.HttpProtocolTypeProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.NAME_RESOLVED_FLAG;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

@Data
@AllArgsConstructor
public class NameResolveClientHttpRequestInterceptor implements ClientHttpRequestInterceptor, Ordered {

  private HttpProtocolTypeProperties protocolTypeProperties;

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    Map<String, Object> attributes = request.getAttributes();
    Object nameResolvedFlag = attributes.get(NAME_RESOLVED_FLAG);
    if(nameResolvedFlag == null || Boolean.TRUE.equals(nameResolvedFlag)) {
      return execution.execute(request, body);
    }

    String serviceName = (String)attributes.get(SERVICE_NAME);
    String channelName = (String)attributes.get(CHANNEL_NAME);

    NameResolver.Result result = NameResolver.resolve(serviceName, channelName, protocolTypeProperties);

    Boolean nameResolved = result.getNameResolved();
    String resolvedAddress = result.getResolvedAddress();
    if(resolvedAddress != null) {
      attributes.put(NAME_RESOLVED_FLAG, nameResolved);

      URI uri = request.getURI();
      URI parsedURI = URI.create(resolvedAddress);
      int port = parsedURI.getPort() > -1 ?parsedURI.getPort():uri.getPort();
      URI newUri = UriComponentsBuilder.fromUri(uri)
          .scheme(parsedURI.getScheme())
          .host(parsedURI.getHost())
          .port(port)
          .replacePath(parsedURI.getPath())
          .path(uri.getPath())
          .build()
          .toUri();
      return execution.execute(new HttpRequest() {

        @Override
        public HttpHeaders getHeaders() {
          return request.getHeaders();
        }

        @Override
        public HttpMethod getMethod() {
          return request.getMethod();
        }

        @Override
        public URI getURI() {
          return newUri;
        }

        @Override
        public Map<String, Object> getAttributes() {
          return request.getAttributes();
        }
      }, body);
    }

    return execution.execute(request, body);
  }

  @Override
  public int getOrder() {
    return -10;
  }
}
