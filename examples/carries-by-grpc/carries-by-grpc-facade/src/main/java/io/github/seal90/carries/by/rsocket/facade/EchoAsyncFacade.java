package io.github.seal90.carries.by.rsocket.facade;

import grpc.examples.echo.EchoOuterClass;
import io.grpc.stub.StreamObserver;
import org.springframework.messaging.handler.annotation.MessageMapping;

@MessageMapping("grpc.examples.echo.EchoGrpc$EchoStub")
public interface EchoAsyncFacade {

    void unaryEcho(EchoOuterClass.EchoRequest request,
                   StreamObserver<EchoOuterClass.EchoResponse> responseObserver);

    void serverStreamingEcho(EchoOuterClass.EchoRequest request,
                                     StreamObserver<EchoOuterClass.EchoResponse> responseObserver);

    StreamObserver<EchoOuterClass.EchoRequest> clientStreamingEcho(
            StreamObserver<EchoOuterClass.EchoResponse> responseObserver);

    StreamObserver<EchoOuterClass.EchoRequest> bidirectionalStreamingEcho(
            StreamObserver<EchoOuterClass.EchoResponse> responseObserver);
}
