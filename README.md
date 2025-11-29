# Service Client

Welcome to the **Service Client** project!

Built on the "service name + interface contract" model, clients can invoke remote services as if they were local methods: service discovery is driven by the service name, invocation behavior is governed by the shared interface, and all communication details—such as protocol, serialization, and connection management—are transparently encapsulated, leaving business code completely unaware.

* **Interfaces are shared and reused between client and server** — eliminating inconsistencies in fields or type mismatches at the source.
* **Service names are explicitly used as invocation identifiers** — making dependencies clear and significantly reducing integration and maintenance costs.

Here, "services" encompass not only internal microservices within a cluster but also APIs provided by external systems. We unify all remote dependencies under a service-oriented abstraction, enabling a consistent access model for both internal and external interfaces.

To simplify multi-protocol integration, the project provides a unified client annotation: `@ServiceClient`. Regardless of whether the backend uses gRPC, HTTP, or another protocol, only minimal configuration is needed to automatically initialize the client and enable seamless, type-safe remote calls.

# Core Elements of Service Invocation

To invoke a target service, the caller must explicitly specify the following four core elements:

* **Service Name**: The logical identifier of the target service, typically bound to a communication channel and used for service discovery and routing.
* **Channel Configuration**: Defines the communication mechanism, including endpoint address, timeout policies, protocol type, and serialization/deserialization strategies.
* **Communication Payload**: The actual business data being transmitted, comprising the agreed-upon data model (e.g., field structure) and its concrete content.
* **Additional Metadata**: Contextual information used to control or assist communication, such as HTTP method and path, authentication tokens, tenant identifiers, or routing hints required by the service registry.

# The Four Elements of the `@ServiceClient` Annotation

- **`protocol`**  
  Specifies the communication protocol and its implementation, in the format `protocol_implementation` (e.g., `HTTP_FEIGN`). By convention, Spring omits the implementation suffix and uses only the protocol name (e.g., `GRPC`, `HTTP`).  
  This attribute determines which underlying communication component to use. A global default can be configured; if not explicitly set, the default value is `GRPC`.

- **`serviceName`**  
  The logical name of the target service, applicable to both internal microservices and external third-party services.  
  When `channelName` is not configured, the system performs service discovery based on `serviceName` as follows:
    1. **First checks static address mappings in local configuration files**;
    2. **If no match is found and a service registry is enabled**, resolves service instances via service discovery mechanisms (e.g., Consul, Nacos).
  > Analogous to DNS resolution: check local `hosts` first, then query remote DNS.

- **`channelName`**  
  Explicitly specifies the target communication channel when it differs from `serviceName`. Supports multiple prefix-based syntaxes:

| Prefix | Behavior |
|--------|----------|
| `static://` | Treats the remainder as a direct target address (supports HTTP/HTTPS or Unix sockets)<br>Examples:<br>`static://http://www.spring.io`<br>`static://unix:///path/to/socket` |
| `lb://` | Resolves the service name via client-side load balancing<br>Example: `lb://USER-SERVICE` |
| `context://` | Retrieves a pre-registered channel bean by name from the Spring `ApplicationContext` |
| `default://` | Uses the default channel configuration defined in the application properties |
| `channel://` or **no prefix** | Loads a custom channel configuration from the configuration file using the given name |

- **`interceptors`**  
  Allows injection of custom interception logic. Common use cases include request header manipulation, authentication token injection, and call logging.  
  Within an interceptor, the following constants can be used to retrieve annotation metadata from the request context (`attributes`):

    - `ProtocolClientAnnotationBeanPostProcessor.SERVICE_NAME` → retrieves the `serviceName`
    - `ProtocolClientAnnotationBeanPostProcessor.CHANNEL_NAME` → retrieves the `channelName`

# Using `@ServiceClient`

- **Service Discovery via `serviceName`**  
  Use a logical service name to trigger automatic service discovery (e.g., through a service registry), dynamically resolving the target instance address.

  ```java
  @ServiceClient(serviceName = "HelloWorldService")
  private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
  ```

- **Invoking Services via the Default Channel**  
  When the channel is specified as `default://`, the call uses the default communication channel.

  ```java
  @ServiceClient(serviceName = "HelloWorldService", channelName = "default://")
  private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
  ```

- **Calling via a Specific Predefined Channel (Server-Side Discovery)**  
  Explicitly reference a predefined channel that encapsulates service discovery, load balancing, or proxy logic—ideal for advanced routing or governance scenarios.

  ```java
  @ServiceClient(serviceName = "HelloWorldService", channelName = "myChannel")
  // or channel prefix 
  // @ServiceClient(serviceName = "HelloWorldService", channelName = "channel://myChannel")
  private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
  ```

- **Cross-Cluster Service Invocation (via an Intermediary Service)**  
  Use a known intermediary service as a gateway to access a target service located in another cluster, network partition, or security domain—common in multi-cluster or hybrid-cloud architectures.

  ```java
  @ServiceClient(serviceName = "HelloWorldService", channelName = "lb://proxyServer")
  private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
  ```

- **Direct Invocation via Static Address**  
  Bypass service discovery entirely by using the `static://` prefix to specify a concrete target address (e.g., an HTTP URL or Unix socket). Suitable for third-party APIs, external systems, or debugging.

  ```java
  @ServiceClient(serviceName = "HelloWorldService", channelName = "static://http://www.github.io")
  private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
  ```

- **Communication via a Custom `WebClient` / `RestTemplate` Instance**  
  Leverage a custom HTTP client bean for fine-grained control over connections, interceptors, or protocol behavior in advanced use cases.

  ```java
  @ProtocolClient(serviceName = "HelloWorldService", channelName = "context://myBean")
  private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
  ```

# Supported Protocols

- GRPC
- HTTP
- HTTP_FEIGN

# Core Feature Overview

## 1. Service Discovery via Local Configuration Files

> **Service address resolution follows a three-level priority strategy**:
> 1. **First, use the explicitly specified channel configuration in `channelName`** (e.g., `static://`, `lb://`, etc.);
> 2. **Next, attempt to resolve a static address from local configuration files or trigger service discovery based on `serviceName`**;
> 3. **If no target address is resolved yet, fall back to directly extracting the target address from the request URL**.
>
> This design ensures that even when `serviceName` and `channelName` are absent or unresolvable, subsequent processing can proceed using the resolved URL—without needing to interpret the higher-level naming logic—thereby enhancing the robustness and compatibility of the invocation pipeline.

- **Trigger Condition**: If the `PROTOCOL_CLIENT_NAME_RESOLVED` attribute is not present in the request context, it indicates that the service name has not yet been resolved. In this case, the system automatically attempts to match the request URL against static addresses defined in local configuration files (e.g., `application.yml`).
- **Subsequent Resolution**: Once this stage completes, downstream service discovery logic no longer needs to consider `serviceName` or `channelName`; it can proceed using only the resolved URL, simplifying the invocation chain.
- **Execution Order**: Implemented by a dedicated interceptor and ordered via `AnnotationAwareOrderComparator` with priority set to **`order = -10`**, ensuring resolution occurs before any other service discovery or invocation logic.

## 2. Header Propagation
To implicitly propagate contextual information—such as user identity or region—from upstream to downstream services, the system supports automatic forwarding of specified HTTP headers.

- **Typical Use Cases**: Forwarding business- or governance-related headers like `X-User-ID`, `X-Tenant-ID`, or `X-Region`.
- **Execution Timing**: Handled by a separate interceptor, ordered via `AnnotationAwareOrderComparator` with priority **`order = 10`**, ensuring headers are injected after address resolution but before the actual remote call is made.
- **Configurability**: The list of headers to propagate can be customized via the following configuration property:
  ```yaml
  seal.spring.service-client.http.forward-web-headers: []
  ```
  By default, this list is empty, meaning no headers are automatically forwarded. To enable propagation, simply specify the desired header names (e.g., `["Authorization", "X-Trace-ID"]`).

# Configuration Management

## Default Configuration and Shared Reuse

Within a cluster, configurations such as header propagation rules and default channels are typically consistent across services. To avoid duplication and improve maintainability, it is recommended to encapsulate these common settings into a **shared dependency library** that all business applications can uniformly reference.

- **Configuration Precedence and Override Mechanism**  
  Spring Boot natively supports multi-layered configuration loading, enabling a “shared defaults + per-service overrides” model:
  - Common configurations can be packaged in `src/main/resources/application.yml` (or `.properties`) within the shared library;
  - Business applications can provide overriding or customized configurations under `src/main/resources/config/`;
  - Spring Boot’s configuration loading order ensures that files under `config/` take **higher precedence**, safely overriding default values without unintentionally replacing unrelated settings.

- **Additional Extension Options**
  - Runtime customization can be achieved by mounting external configuration files during image build or deployment (e.g., via ConfigMaps, environment variables, or volume mounts);
  - Dynamic inclusion of remote or shared configuration sources is also supported using `spring.config.import`.

## Configuration File Formats

```yaml
seal:
  spring:
    service-client:
      default-protocol: grpc
      grpc:
        forward-grpc-metadata:
          - overlay-ns
        default-channel-config:
          load-balancing-policy: "round_robin"
          negotiation-type: PLAINTEXT # TLS, PLAINTEXT_UPGRADE, PLAINTEXT;
          enable-keep-alive: false
          idle-timeout: 20s
          keep-alive-time: 5s
          keep-alive-timeout: 20s
          keep-alive-without-calls: false
          max-inbound-message-size: 4M
          max-inbound-metadata-size: 8K
          user-agent: 
          default-deadline: 
          secure: true
          ssl-bundle: ""
        default-channel-name:
        channels:
          CHANNEL-NAME:
            addresses:
              - ip:port
              - ip:port
            load-balancing-policy: "round_robin"
            negotiation-type: PLAINTEXT # TLS, PLAINTEXT_UPGRADE, PLAINTEXT;
            enable-keep-alive: false
            idle-timeout: 10s
            keep-alive-time: 10s
            keep-alive-timeout: 10s
            keep-alive-without-calls: false
            max-inbound-message-size: 1M
            max-inbound-metadata-size: 1M
            user-agent:
            default-deadline:
            secure: false
            ssl-bundle: ""              
        services:
          SERVICE-NAME:
            channel-name:
            channel-config:
              addresses:
                - ip:port
                - ip:port
---

seal:
  spring: 
    service-client:
      http-spring:
        forward-web-headers: # Forward the HTTP request headers from the web server 
          - overlay-ns
#        default-channel-config:
        default-channel-name:
        channels:
          CHANNEL-NAME:
            address: https://www.github.com
            addresses:
              - https://www.github.com
              - https://www.github.io
# WebClient.Builder nonsupport
#            redirects: DONT_FOLLOW # FOLLOW_WHEN_POSSIBLE, FOLLOW, DONT_FOLLOW
#            connect-timeout: 5s
#            read-timeout: 10s
#            # call-timeout write-timeout
#            ssl-bundle: ""

        services:
          SERVICE-NAME:
            channel-name:
            channel-config:
              addresses:
                - https://www.github.com
                - https://www.github.io
#              default-headers:
#                - HEADER-KEY: [HEADER-VALUE1, HEADER-VALUE2]

```

# Example

> client --> Server --> Server itself(mocker other)

* gRPC Example: [grpc](examples/grpc)
* HTTP WebClient Example: [http-webclient](examples/http-webclient)
* HTTP RestTemplate Example: [http-resttemplate](examples/http-resttemplate)
* HTTP Feign Example: [http-feign](examples/http-feign)

# TODO

- **Unified gRPC Configuration Loading Mechanism**  
  Provide a standardized approach to load and parse gRPC client/server configurations—such as timeouts, interceptors, and channel parameters—with dynamic injection support from `application.yml` or external configuration sources, eliminating hard-coded values.

- **Enhanced Extensibility for gRPC Clients**  
  Inspired by the fluent and customizable design of `WebClient.Builder`, introduce a flexible Builder pattern for gRPC clients that enables on-demand registration of interceptors, adjustment of connection policies, codec swapping, and more—improving both developer experience and runtime control.

- **Refactored Dependency Management via a Dedicated Starter**  
  Introduce a standalone Spring Boot Starter (e.g., `seal-spring-boot-starter-grpc`) that encapsulates all gRPC-related dependencies, auto-configuration classes, and default behaviors. This delivers an "out-of-the-box" experience while preventing version conflicts and transitive dependency pollution.