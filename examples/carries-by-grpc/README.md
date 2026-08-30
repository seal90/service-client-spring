gRPC
-   **Current User Handling:** The interceptor stores user information in the `Context`, and the Service retrieves it from the `Context`.
-   **Service Metadata Handling:** Metadata processing within the Service requires coordination with the `Context` inside an interceptor.
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
