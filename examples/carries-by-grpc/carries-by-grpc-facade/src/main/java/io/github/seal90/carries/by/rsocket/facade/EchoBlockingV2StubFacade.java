package io.github.seal90.carries.by.rsocket.facade;


import grpc.examples.echo.EchoOuterClass;
import io.grpc.stub.BlockingClientCall;
import org.springframework.messaging.handler.annotation.MessageMapping;

@MessageMapping("grpc.examples.echo.Echo")
public interface EchoBlockingV2StubFacade {

    @MessageMapping("/UnaryEcho")
    public EchoOuterClass.EchoResponse unaryEcho(EchoOuterClass.EchoRequest request);

    @MessageMapping("/ServerStreamingEcho")
    public BlockingClientCall<?, EchoOuterClass.EchoResponse>
    serverStreamingEcho(EchoOuterClass.EchoRequest request);

    @MessageMapping("/ClientStreamingEcho")
    public BlockingClientCall<EchoOuterClass.EchoRequest, EchoOuterClass.EchoResponse>
    clientStreamingEcho();

    @MessageMapping("/BidirectionalStreamingEcho")
    public BlockingClientCall<EchoOuterClass.EchoRequest, EchoOuterClass.EchoResponse>
    bidirectionalStreamingEcho();

}
