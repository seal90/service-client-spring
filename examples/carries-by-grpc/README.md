gRPC
-   **Current User Handling:** The interceptor stores user information in the `Context`, and the Service retrieves it from the `Context`.
-   **Service Metadata Handling:** Metadata processing within the Service requires coordination with the `Context` inside an interceptor.
    ```java
    public static final Context.Key<Metadata> SERVER_REQUEST_HEADER_KEY = Context.key("grpc.server.request.metadata");
    public static final Context.Key<Metadata> SERVER_RESPONSE_HEADER_KEY = Context.key("grpc.server.response.metadata");
    
    public ServerInterceptor serverInterceptor() {
    
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                         Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
                Metadata responseMetadata = new Metadata();
                Context context = Context.current().withValue(SERVER_REQUEST_HEADER_KEY, metadata).withValue(SERVER_RESPONSE_HEADER_KEY, responseMetadata);
                ServerCall<ReqT, RespT> newServerCall = new ForwardingServerCall.SimpleForwardingServerCall<>(
                        serverCall) {
    
                    @Override
                    public void sendHeaders(Metadata headers) {
                        Metadata metadata = SERVER_RESPONSE_HEADER_KEY.get();
                        headers.merge(metadata);
                        super.sendHeaders(headers);
                    }
                };
                return Contexts.interceptCall(context, newServerCall, metadata, serverCallHandler);
            }
        };
    }
    
    @Override
    public void serverStreamingEcho(EchoOuterClass.EchoRequest request,
                                    StreamObserver<EchoOuterClass.EchoResponse> responseObserver) {
        Metadata requestMetadata = SERVER_REQUEST_HEADER_KEY.get();
        for(String key : requestMetadata.keys()) {
            log.info("header {}: {}", key, requestMetadata.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)));
        }
        Metadata responseMetadata = SERVER_RESPONSE_HEADER_KEY.get();
        responseMetadata.put(Metadata.Key.of("x-custom-response-header", Metadata.ASCII_STRING_MARSHALLER), "custom-response-value");
        EchoOuterClass.EchoResponse echoResponse = EchoOuterClass.EchoResponse.newBuilder().setMessage("unaryEcho").build();
        responseObserver.onNext(echoResponse);
        responseObserver.onNext(echoResponse);
        responseObserver.onCompleted();
    }
    ```
-   **Client Metadata Handling:** Leverage `CallOptions.Key<T>` and `ClientInterceptor` to send metadata and receive responses.
    ```java
    public static final CallOptions.Key<Metadata> CLIENT_HEADER_REQUEST_KEY = CallOptions.Key.create("grpc.client.request.Metadata");
    public static final CallOptions.Key<AtomicReference<Metadata>> CLIENT_HEADER_RESPONSE_KEY = CallOptions.Key.create("grpc.client.response.Metadata");
    
    public ClientInterceptor clientInterceptor() {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                                       CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                        next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener responseListener, Metadata headers) {
                        Metadata metadata = callOptions.getOption(CLIENT_HEADER_REQUEST_KEY);
                        if(metadata != null) {
                            headers.merge(metadata);
                        }
                        super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                                responseListener) {
                            @Override
                            public void onHeaders(Metadata headers) {
                                AtomicReference<Metadata> headerRef = callOptions.getOption(CLIENT_HEADER_RESPONSE_KEY);
                                if (headerRef != null) {
                                    headerRef.set(headers);
                                }
                                super.onHeaders(headers);
                            }
                        }, headers);
                    }
                };
            }
        };
    }
    
    private void unaryEchoCall() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Metadata> headerRef = new AtomicReference<>();
    
        EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("unaryEcho").build();
        StreamObserver<EchoOuterClass.EchoResponse> responseObserver = new StreamObserver<>(){
    
            @Override
            public void onNext(EchoOuterClass.EchoResponse echoResponse) {
                Metadata metadata = headerRef.get();
                for(String key : metadata.keys()) {
                    log.info("header {}: {}", key, metadata.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)));
                }
                log.info("stub unaryEcho response: {}", echoResponse.getMessage());
            }
    
            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
    
            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };
    
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("x-custom-header", Metadata.ASCII_STRING_MARSHALLER), "custom-value");
        echoStub.withOption(CLIENT_HEADER_REQUEST_KEY, metadata).withOption(CLIENT_HEADER_RESPONSE_KEY, headerRef).unaryEcho(echoRequest, responseObserver);
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    ```
-   **Error Code Mechanism:** Under normal conditions, responses are successful. When errors occur, error details are returned via metadata; on the client side, these can be extracted from the caught `StatusException` or `StatusRuntimeException`.
    ```java
    Metadata.Key<String> BIZ_ERROR_CODE = Metadata.Key.of("business-error-code", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> BIZ_ERROR_MSG = Metadata.Key.of("business-error-message", Metadata.ASCII_STRING_MARSHALLER);
    Metadata metadata = new Metadata();
    metadata.put(BIZ_ERROR_CODE, "MOCK_ERROR");
    metadata.put(BIZ_ERROR_MSG, "mock error");
    responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Validation failed")
                    .asException(metadata));
    ```


spring-grpc
GrpcClientFactory#getClient options add target: ${target}