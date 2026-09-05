Initially I want to simplify API Invocation via Client Injection
- grpc [GrpcClientApplication.java](examples/grpc/grpc-client/src/main/java/io/github/seal90/grpc_client/GrpcClientApplication.java)
- http [HttpClientWebClientApplication.java](examples/http-webclient/http-client/src/main/java/io/github/seal90/http_client/HttpClientWebClientApplication.java)
- mqtt [MqttEclipseClientApplication.java](examples/mqtt-eclipse/mqtt-eclipse-client/src/main/java/io/github/seal90/mqtt_eclipse_client/MqttEclipseClientApplication.java)
- rsocket [RSocketClientApplication.java](examples/rsocket/rsocket-client/src/main/java/io/github/seal90/rsocket_client/RSocketClientApplication.java)

Then, this implementation is fully transparent to users, automatically converting diverse external protocol interfaces into a high-efficiency protocol for intra-cluster communication. For example, HTTP API requests can be transported over RSocket, maintaining external compatibility while improving internal transmission performance.
- rsocket carries http [HttpClientWebClientApplication.java](examples/carries-http/carries-http-client/src/main/java/io/github/seal90/http_client/HttpClientWebClientApplication.java)

Ultimately, all invocations are unified under a single protocol, with the Proxy handling protocol translation. To the service, the Proxy serves as the sole entry and exit point, completely abstracting away upstream and downstream protocol differences.
- by rsocket [CarriesByRSocketClientApplication.java](examples/carries-by-rsocket/carries-by-rsocket-client/src/main/java/io/github/seal90/carries/by/rsocket/CarriesByRSocketClientApplication.java)
- by grpc [CarriesByGrpcClientApplication.java](examples/carries-by-grpc/carries-by-grpc-client/src/main/java/io/github/seal90/carries/by/grpc/client/CarriesByGrpcClientApplication.java)
- the proxy https://github.com/servicefilter/servicefilter-go

Service meshes such as Istio and Linkerd, along with Dapr, fundamentally rely on server-side service discovery mechanisms. They require the target service to be explicitly declared via annotations or metadata to enable correct routing by the middleware.

In contrast, current Spring gRPC implementations (e.g., `ImportGrpcClients`) remain based on client-side discovery. The `target` configuration is bound solely at the Channel level and is not propagated to CallOptions. Consequently, the invocation request lacks the necessary target service identifier, preventing server-side discovery mechanisms from determining the actual destination of the call.
