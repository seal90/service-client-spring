# RPC data : Metadata & Payload
Metadata carries the control semantics of an invocation, while Payload carries the business semantics.

Business logic should not normally depend directly on metadata. However, practical scenarios inevitably arise where accessing metadata is justified—for example, retrieving the current user identity from the request context or returning a filename via response headers. When a framework does not expose explicit metadata access primitives, developers are forced to resort to implicit conventions between the Context and Interceptors, which increases both cognitive overhead and maintenance burden.

Developers are expected to have full awareness of the behavior and implications of their code. Accordingly, frameworks should provide controlled metadata access mechanisms:

- Incoming Metadata (Request): Read-only. As contextual facts supplied by the caller, incoming metadata must be readable but immutable within the business layer, ensuring consistency and traceability of invocation semantics.
- Outgoing Metadata (Response): Writable. Business logic or middleware may attach control information as needed (e.g., pagination tokens, caching directives, file identifiers), serving as an orthogonal complement to the response payload.



## Typical API Design

* Payload Data-Only API: [HelloWorldFacade.java](carries-by-rsocket-facade/src/main/java/io/github/seal90/carries/by/rsocket/facade/HelloWorldFacade.java)
* Handle Metadata in Parameters: [HelloWorldWithMetadataFacade.java](carries-by-rsocket-facade/src/main/java/io/github/seal90/carries/by/rsocket/facade/HelloWorldWithMetadataFacade.java)
* Use custom request/response: [HelloWorldWithBoxFacade.java](carries-by-rsocket-facade/src/main/java/io/github/seal90/carries/by/rsocket/facade/HelloWorldWithBoxFacade.java)
* Use any as body: [HelloWorldWithMessageFacade.java](carries-by-rsocket-facade/src/main/java/io/github/seal90/carries/by/rsocket/facade/HelloWorldWithMessageFacade.java)
* Wrap any with Generics: [HelloWorldWithMessageBoxFacade.java](carries-by-rsocket-facade/src/main/java/io/github/seal90/carries/by/rsocket/facade/HelloWorldWithMessageBoxFacade.java)
* Handle mq: [MqCarriesByRSocketFacade.java](carries-by-rsocket-facade/src/main/java/io/github/seal90/carries/by/rsocket/facade/MqCarriesByRSocketFacade.java)
* Handle MySQL: [MysqlCrudFacade.java](carries-by-rsocket-facade/src/main/java/io/github/seal90/carries/by/rsocket/facade/MysqlCrudFacade.java)
* Handle transaction: [CarriesByRSocketClientApplication.java](carries-by-rsocket-client/src/main/java/io/github/seal90/carries/by/rsocket/CarriesByRSocketClientApplication.java#mysqlCrudFacadeCall)

## Request Response

```java
class Request {
    private Map<String, Object> attributes; // 
    private Map<String, Any> metadata; // Unmodifiable
    private com.google.protobuf.Any data; // Change to byte[] ?
}

class Request {
    private Map<String, Any> metadata;
    private com.google.protobuf.Any data; // Change to byte[] ?
}

public <T extends Message> T fromBytes(byte[] bytes, Class<T> clazz) throws Exception {
    Method method = clazz.getMethod("parseFrom", byte[].class);
    return clazz.cast(method.invoke(null, bytes));
}
```

```text
client: build Request -> rpc data: metadata: Request.metadata payload: Request.data -> to server

server: rpc data: metadata: Request.metadata payload: Request.data -> Request

server: build Response -> rpc data: metadata: Response.metadata payload: Response.data -> to client

client: rpc data: metadata: Response.metadata payload: Response.data -> Response
```

# Startup Process
* Start
* Pull configuration from carries-server
* Send service availability information

# Provision of Common Capabilities

-   **Configuration Management:** Fetch configuration on startup; refresh on change
-   **ID Generation**
-   **Concurrency Locking:** Configure local locking for single-node deployments; distributed locking for clustered deployments
-   **Scheduled Tasks**
-   **Header Propagation**
-   **Messaging & Service Invocation**
-   **Database & Transaction Management**
-   **Testing Support:** Context-aware automatic stubbing/mocking
-   **Tracing Context**


# Pending Decisions & Technology Selection Notes

## Communication Protocol Selection: gRPC Preferred Over RSocket

-   **Theoretical Advantages of RSocket:** Supports request handling without a listening endpoint and avoids partial overhead of the HTTP/2 protocol stack.
-   **Reasons for Deprecation:** Declining community activity and immature Go ecosystem implementation; the reactive programming model introduces significant cognitive and debugging overhead, resulting in unpredictable team adoption costs.
-   **Current Decision:** Adopt **gRPC** as the primary communication protocol to balance ecosystem maturity.

## Protobuf Dynamic Field Type: `repeated bytes` Preferred Over `google.protobuf.Any`

-   **Limitations of `Any`:** Requires additional type descriptor registration and cross-language type mapping table maintenance, increasing serialization/deserialization complexity and runtime overhead.
-   **Rationale for `repeated bytes`:** Following practices from similar projects such as Dapr, raw byte arrays are used to carry dynamic payloads, avoiding type system coupling. This results in shorter encoding/decoding paths, making it more suitable for pass-through/routing components like Proxy.
