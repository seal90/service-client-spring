**Client-Side Discovery:** Client (Name Resolution) → Server
The client incorporates built-in service discovery logic, resolving service names to specific addresses independently before connecting directly to the server.

**Server-Side Discovery:** Client → Proxy (Name Resolution) → Server
The client connects solely to a fixed proxy address, while the proxy layer handles service name resolution and request routing.

First: I want to simple service call api by client inject
Then: I want to simple client app by only one protocol
Then: I want to simple client app by proxy(sidecar) app

**Simplify API Invocation via Client Injection:** Streamline service calls by injecting client dependencies automatically.
**Unify Client Communication Protocol:** Standardize the client app on a single protocol to reduce complexity.
**Decouple Networking via Sidecar Proxy:** Offload cross-cutting concerns by routing traffic through a sidecar proxy.

Initially I want to simplify API Invocation via Client Injection
- grpc [GrpcClientApplication.java](examples/grpc/grpc-client/src/main/java/io/github/seal90/grpc_client/GrpcClientApplication.java)
- http [HttpClientWebClientApplication.java](examples/http-webclient/http-client/src/main/java/io/github/seal90/http_client/HttpClientWebClientApplication.java)
- mqtt [MqttEclipseClientApplication.java](examples/mqtt-eclipse/mqtt-eclipse-client/src/main/java/io/github/seal90/mqtt_eclipse_client/MqttEclipseClientApplication.java)
- rsocket [RSocketClientApplication.java](examples/rsocket/rsocket-client/src/main/java/io/github/seal90/rsocket_client/RSocketClientApplication.java)

然后我想上面的实现可以让用户不感知具体实现,这样以多种协议的调用接口可以转为高效协议在集群内通信.例如用rsocekt 承载http协议接口实现
然后，该实现对用户完全透明，能够将多种外部协议接口自动转换为高效协议进行集群内通信。例如，通过 RSocket 承载 HTTP 接口请求，在保持对外兼容的同时提升内部传输效率。
Then, this implementation is fully transparent to users, automatically converting diverse external protocol interfaces into a high-efficiency protocol for intra-cluster communication. For example, HTTP API requests can be transported over RSocket, maintaining external compatibility while improving internal transmission performance.
- rsocket carries http [HttpClientWebClientApplication.java](examples/carries-http/carries-http-client/src/main/java/io/github/seal90/http_client/HttpClientWebClientApplication.java)
最后我想可以将所有的调用统一为同一协议,由proxy来实现协议的转换.在服务看来只有一个外部即proxy
最终，将所有调用统一为单一协议，由 Proxy 负责协议转换。对服务而言，Proxy 是唯一的出入口，从而彻底屏蔽上下游协议的差异。
Ultimately, all invocations are unified under a single protocol, with the Proxy handling protocol translation. To the service, the Proxy serves as the sole entry and exit point, completely abstracting away upstream and downstream protocol differences.
- by rsocket [CarriesByRSocketClientApplication.java](examples/carries-by-rsocket/carries-by-rsocket-client/src/main/java/io/github/seal90/carries/by/rsocket/CarriesByRSocketClientApplication.java)
- by grpc [CarriesByGrpcClientApplication.java](examples/carries-by-grpc/carries-by-grpc-client/src/main/java/io/github/seal90/carries/by/grpc/client/CarriesByGrpcClientApplication.java)
- the proxy https://github.com/servicefilter/servicefilter-go
目前spring提供的实现是客户端发现,例如 `ImportGrpcClients` 的 target 对应的配置是 channel,且未将 target 放到 callOption 中,导致无法告知服务端发现的服务要调用的目标服务
istio/linkerd 的服务网格方案, dapr的方案都需要一个服务端发现的注解.
servicefilter-go与dapr的想法很相似.servicefilter-go 将服务放在第一位, dapr首先区分的是服务类型,例如 store invocation
servicefilter-go实现完成后发现此实现再次认同多年前的一个实现 https://github.com/servicefilter/servicefilter-rust (^_^;)

Istio/Linkerd 等服务网格与 Dapr 本质上均采用服务端服务发现机制，需要通过注解或元数据显式声明目标服务，以便中间件正确路由。 
目前 Spring 提供的 gRPC 实现（如 ImportGrpcClients）仍基于客户端发现模式。其 target 配置仅绑定至 Channel 层级，并未透传至 CallOption，导致调用请求中缺失目标服务标识，使得服务端发现机制无法获知真实的调用目标。
