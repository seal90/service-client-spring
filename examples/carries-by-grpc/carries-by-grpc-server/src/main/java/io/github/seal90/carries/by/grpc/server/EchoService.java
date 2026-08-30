package io.github.seal90.carries.by.grpc.server;

import grpc.examples.echo.EchoGrpc;
import grpc.examples.echo.EchoOuterClass;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EchoService extends EchoGrpc.EchoImplBase {

    @Override
    public void unaryEcho(EchoOuterClass.EchoRequest request,
                          StreamObserver<EchoOuterClass.EchoResponse> responseObserver) {
        log.info("unaryEcho request: {}", request);
//        EchoOuterClass.EchoResponse echoResponse = EchoOuterClass.EchoResponse.newBuilder().setMessage("unaryEcho").build();
//        responseObserver.onNext(echoResponse);
//        responseObserver.onCompleted();

        Metadata.Key<String> BIZ_ERROR_CODE = Metadata.Key.of("business-error-code", Metadata.ASCII_STRING_MARSHALLER);
        Metadata.Key<String> BIZ_ERROR_MSG = Metadata.Key.of("business-error-message", Metadata.ASCII_STRING_MARSHALLER);
        Metadata metadata = new Metadata();
        metadata.put(BIZ_ERROR_CODE, "MOCK_ERROR");
        metadata.put(BIZ_ERROR_MSG, "mock error");

        responseObserver.onError(Status.FAILED_PRECONDITION
                .withDescription("Validation failed")
                .asException(metadata));
    }

    @Override
    public void serverStreamingEcho(EchoOuterClass.EchoRequest request,
                                     StreamObserver<EchoOuterClass.EchoResponse> responseObserver) {
        log.info("serverStreamingEcho request: {}", request);
        EchoOuterClass.EchoResponse echoResponse = EchoOuterClass.EchoResponse.newBuilder().setMessage("unaryEcho").build();
        responseObserver.onNext(echoResponse);
        responseObserver.onNext(echoResponse);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<EchoOuterClass.EchoRequest> clientStreamingEcho(
            StreamObserver<EchoOuterClass.EchoResponse> responseObserver) {
        return new StreamObserver<>() {

            private final List<EchoOuterClass.EchoRequest> requestList = new ArrayList<>();

            @Override
            public void onNext(EchoOuterClass.EchoRequest request) {
                log.info("clientStreamingEcho request: {}", request);
            }

            @Override
            public void onCompleted() {
                log.info("clientStreamingEcho {} times", requestList.size());

                EchoOuterClass.EchoResponse resp = EchoOuterClass.EchoResponse.newBuilder()
                        .setMessage("clientStreamingEcho receive" + requestList.size() + "times").build();

                responseObserver.onNext(resp);
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable t) {
                log.error("clientStreamingEcho client error", t);
                responseObserver.onError(t);
            }
        };
    }

    @Override
    public StreamObserver<EchoOuterClass.EchoRequest> bidirectionalStreamingEcho(
            StreamObserver<EchoOuterClass.EchoResponse> responseObserver) {
        return new StreamObserver<>() {

            private final List<EchoOuterClass.EchoRequest> requestList = new ArrayList<>();

            @Override
            public void onNext(EchoOuterClass.EchoRequest request) {
                log.info("bidirectionalStreamingEcho request: {}", request);
            }

            @Override
            public void onCompleted() {
                log.info("bidirectionalStreamingEcho {} times", requestList.size());

                EchoOuterClass.EchoResponse resp = EchoOuterClass.EchoResponse.newBuilder()
                        .setMessage("bidirectionalStreamingEcho receive" + requestList.size() + "times").build();

                responseObserver.onNext(resp);
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable t) {
                log.error("bidirectionalStreamingEcho client error", t);
                responseObserver.onError(t);
            }
        };
    }
}
