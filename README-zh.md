# Service Client

欢迎使用 Service Client 项目！

依托“服务名 + 接口契约”模型，客户端可像调用本地方法一样访问远程服务：服务发现由名称驱动，调用行为由共享接口约束，所有通信细节（如协议、序列化、连接管理）均被透明封装，业务代码零感知。
* `接口定义在客户端与服务端共享复用` - 从源头杜绝字段不一致、类型错配等问题
* `显式使用服务名称作为调用标识` - 使依赖关系清晰可见，显著降低联调与维护成本

此处的“服务”不仅涵盖集群内部微服务，也包括外部系统提供的 API——我们将所有远程依赖统一抽象为服务维度，实现内外部接口的一致访问模型。

为简化多协议接入，项目提供了统一的客户端注解 @ServiceClient。无论后端是 gRPC、HTTP 还是其他协议，只需少量配置，即可自动完成客户端初始化，实现无缝、类型安全的远程调用。

# 服务调用核心要素

调用目标服务，调用方需明确指定以下四个核心要素：

* 服务名称（Service Name）：目标服务的逻辑标识，通常与通信通道绑定，用于服务发现与路由。
* 通道配置（Channel Configuration）：定义通信方式，包括端点地址、超时策略、协议类型及序列化/反序列化机制。
* 通信载荷（Communication Payload）：实际传输的业务数据，包含双方约定的数据模型（如字段结构）及其具体内容。
* 附加元数据（Additional Metadata）：用于控制或辅助通信的上下文信息，例如 HTTP 方法与路径、认证令牌、租户标识，或服务注册中心所需的路由提示。

# `@ServiceClient` 注解的四个元素

- **`protocol`**  
    指定通信协议及其实现方式，格式为 `协议_实现`（如 `HTTP_FEIGN`）。Spring 默认省略实现后缀，仅使用协议名（如 `GRPC`、`HTTP`）。  
    该属性用于选择底层通信组件。支持全局默认配置；若未显式设置，默认值为 `GRPC`。

- **`serviceName`**
    目标服务的逻辑名称，适用于内部微服务与外部第三方服务。  
当未配置 `channelName` 时，系统将基于 `serviceName` 执行服务发现，流程如下：
  1. **优先查找本地配置文件**中的静态地址映射；
  2. **若未命中且启用了注册中心**，则通过服务发现机制（如 Consul、Nacos）解析服务实例；
   > 类比 DNS 解析：先查本地 hosts，再查远程 DNS。

- **`channelName`**  
    当实际调用通道与 `serviceName` 不一致时，可通过此属性显式指定目标通道。支持多种前缀语法：

| 前缀 | 行为说明 |
|------|--------|
| `static://` | 直接使用后续内容作为目标地址（支持 HTTP/HTTPS/Unix 套接字）<br>示例：<br>`static://http://www.spring.io`<br>`static://unix:///path/to/socket` |
| `lb://` | 通过客户端负载均衡器解析服务名<br>示例：`lb://USER-SERVICE` |
| `context://` | 从 Spring `ApplicationContext` 中按名称获取已注册的通道 Bean |
| `default://` | 使用配置文件中定义的默认通道配置 |
| `channel://` 或 **无前缀** | 从配置文件中加载对应名称的自定义通道配置 |

- **`interceptors`**  
    用于注入自定义拦截逻辑，常见用途包括：请求头处理、认证令牌注入、调用日志记录等。  
    在拦截器中，可通过以下常量从请求上下文（attributes）中获取注解元信息：

  - `ProtocolClientAnnotationBeanPostProcessor.SERVICE_NAME` → 获取 `serviceName`
  - `ProtocolClientAnnotationBeanPostProcessor.CHANNEL_NAME` → 获取 `channelName`

# ServiceClient 使用

- **基于 `serviceName` 的服务发现**  
   使用逻辑服务名触发自动服务发现机制（如注册中心），动态解析目标实例地址。
```java
	@ServiceClient(serviceName = "HelloWorldService")
	private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
```

- **通过 `default channel` 调用服务**  
   当使用 `default://` 指定通道时，则使用默认通道进行调用。
```java
	@ServiceClient(serviceName = "HelloWorldService", channelName="default://")
	private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
```

- **通过特定 Channel 实例调用（服务端发现）**  
   显式指定一个预定义通道，由该通道封装服务发现、负载均衡或代理逻辑，适用于复杂路由或治理场景。

```java
	@ServiceClient(serviceName = "HelloWorldService", channelName="channel://myChannel")
	private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
```

- **跨集群服务调用（通过中介服务）**  
   借助某个已知服务作为跳板，间接访问位于其他集群、网络分区或安全域中的目标服务，常用于多集群、混合云架构。

```java
	@ServiceClient(serviceName = "HelloWorldService", channelName="lb://proxyServer")
	private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
```

- **通过指定静态地址直接调用**  
   绕过服务发现，使用 `static://` 前缀直接指定目标地址（如 HTTP URL 或 Unix 套接字），适用于第三方 API、外部系统或调试用途。

```java
	@ServiceClient(serviceName = "HelloWorldService", channelName="static://www.github.io")
	private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
```

- **使用定制的 `WebClient` / `RestTemplate` 实例通信**  
   使用自定义 HTTP 客户端发起请求，适用于需要精细控制连接、拦截器或协议细节的高级场景。

```java
	@ProtocolClient(serviceName = "HelloWorldService", channelName="context://myBean")
	private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;
```

# 实现的协议
* GRPC
* HTTP
* HTTP_FEIGN

# 核心功能说明

## 1. 基于本地配置文件的服务发现

> **服务地址解析遵循三级优先级策略**：
> 1. **优先使用 `channelName` 中显式指定的通道配置**（如 `static://`、`lb://` 等）；
> 2. **其次基于 `serviceName` 尝试从本地配置文件中匹配静态地址或触发服务发现**；
> 3. **若仍未解析出目标地址，则退化为直接从请求 URL 中提取目标地址**。
>
> 此设计确保即使 `serviceName` 与 `channelName` 未提供或无法解析，后续流程仍可基于已确定的 URL 继续执行，无需再理解上层命名逻辑，提升了调用链路的鲁棒性与兼容性。

- **触发条件**：若请求上下文中未设置 `PROTOCOL_CLIENT_NAME_RESOLVED` 属性，表示服务名尚未解析，此时将自动尝试从本地配置文件（如 `application.yml`）中匹配 URL 对应的静态地址。
- **后续解析**：该阶段完成后，后续的服务发现逻辑无需再关注 `serviceName` 或 `channelName`，仅依据已解析的 URL 即可继续处理，简化了调用链路。
- **执行顺序**：由专用拦截器实现，并通过 `AnnotationAwareOrderComparator` 排序，优先级设为 **`order = -10`**，确保在其他服务发现或调用逻辑之前完成解析。

## 2. Header 透传（Header Propagation）
为实现用户身份、区域等上下文信息的隐式传递，系统支持将上游请求中的指定 HTTP Header 自动透传至下游服务。

- **典型用途**：如传递 `X-User-ID`、`X-Tenant-ID`、`X-Region` 等业务或治理相关字段。
- **执行时机**：由独立拦截器实现，使用 `AnnotationAwareOrderComparator` 排序，优先级设为 **`order = 10`**，确保在地址解析完成后、实际发起远程调用前注入所需 Header。
- **可配置性**：通过如下配置项自定义需透传的 Header 列表：
  ```yaml
  seal.spring.service-client.http.forward-web-headers: []
  ```
  默认为空列表，表示不自动透传任何 Header；按需填入 header 名称（例如 `["Authorization", "X-Trace-ID"]`）即可启用透传。

# 配置管理

## 默认配置与公共复用

在集群内部，Header 透传规则、默认 Channel 等配置通常高度一致。为避免重复配置、提升维护效率，建议将这些通用配置封装到一个**共享依赖包**中，供各业务应用统一引用。

- 配置优先级与覆盖机制  
Spring Boot 原生支持多层级配置加载，天然支持“公共默认 + 业务覆盖”的模式：
  - 公共配置可打包在共享包的 `src/main/resources/application.yml`（或 `.properties`）中；
  - 业务应用可在 `src/main/resources/config/` 目录下提供同名或差异化配置；
  - Spring Boot 的配置加载顺序确保 **`config/` 下的配置具有更高优先级**，可安全覆盖公共默认值，而不会导致非目标配置被意外替换。

- 其他扩展方式
  - 可通过构建镜像时挂载外部配置文件（如 ConfigMap、环境变量或 volume 挂载）实现运行时定制；
  - 也支持使用 `spring.config.import` 动态引入远程或共享配置源。

## 配置文件格式

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

- **统一 gRPC 配置读取机制**  
  提供标准化方式加载和解析 gRPC 客户端/服务端配置（如超时、拦截器、通道参数等），支持从 `application.yml` 或外部配置源动态注入，避免硬编码。

- **增强 gRPC 客户端可扩展性**  
  借鉴 `WebClient.Builder` 的链式构建与定制化设计，为 gRPC 客户端提供灵活的 Builder 模式，支持按需注册拦截器、调整连接策略、切换编解码器等，提升开发体验与运行时控制能力。

- **重构依赖管理，推出专用 Starter**  
  例如创建独立的 Spring Boot Starter（如 `seal-spring-boot-starter-grpc`），封装 gRPC 相关依赖、自动配置类和默认行为，实现“开箱即用”，同时避免版本冲突与传递依赖污染。