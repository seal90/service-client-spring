package io.github.seal90.serviceclient.protocoltypefactory.spring.extension.nameresovle;

import io.github.seal90.serviceclient.protocoltypefactory.spring.extension.properties.HttpProtocolTypeProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.CHANNEL_NAME;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.NAME_RESOLVED_FLAG;
import static io.github.seal90.serviceclient.ServiceClientAnnotationBeanPostProcessor.SERVICE_NAME;

@Data
@AllArgsConstructor
public class NameResolveExchangeFilterFunction implements ExchangeFilterFunction {

  private HttpProtocolTypeProperties protocolTypeProperties;

  @Override
  public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
    Map<String, Object> attributes = request.attributes();
    Object nameResolvedFlag = attributes.get(NAME_RESOLVED_FLAG);
    if(Boolean.TRUE.equals(nameResolvedFlag)) {
      return next.exchange(request);
    }

    String serviceName = (String)attributes.get(SERVICE_NAME);
    String channelName = (String)attributes.get(CHANNEL_NAME);

    NameResolver.Result result = NameResolver.resolve(serviceName, channelName, protocolTypeProperties);
    Boolean nameResolved = result.getNameResolved();
    String resolvedAddress = result.getResolvedAddress();
    if(resolvedAddress != null) {

      URI uri = request.url();
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
      ClientRequest newRequest = ClientRequest.from(request)
          .url(newUri)
          .attribute(NAME_RESOLVED_FLAG, nameResolved)
          .build();
      return next.exchange(newRequest);
    }

    return next.exchange(request);
  }
}
