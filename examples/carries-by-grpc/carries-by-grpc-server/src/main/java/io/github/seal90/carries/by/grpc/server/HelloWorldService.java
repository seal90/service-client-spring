package io.github.seal90.carries.by.grpc.server;

import grpc.examples.echo.EchoOuterClass;
import io.github.seal90.carries.by.rsocket.facade.EchoAsyncFacade;
import io.github.seal90.serviceclient.core.ChannelNamePrefix;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.github.seal90.serviceclient.proto.HelloReply;
import io.github.seal90.serviceclient.proto.HelloRequest;
import io.github.seal90.serviceclient.proto.HelloWorldServiceGrpc;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class HelloWorldService extends HelloWorldServiceGrpc.HelloWorldServiceImplBase {

  @ServiceClient(serviceName = "grpc-server", channelName = ChannelNamePrefix.STATIC_PREFIX+"dns:///127.0.0.1:50051")
  private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;

  @ServiceClient(protocol = "CARRIES_BY_GRPC", channelName = ChannelNamePrefix.STATIC_PREFIX+"dns:///127.0.0.1:50051")
  private EchoAsyncFacade echoAsyncFacade;

  @ServiceClient(protocol = "CARRIES_BY_GRPC_TRANSACTION", channelName = ChannelNamePrefix.STATIC_PREFIX+"dns:///127.0.0.1:50051")
  private TransactionTemplate transactionTemplate;

  @Override
  public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
    log.info("sayHello {}", req.getName());

    transactionTemplate.execute(status -> {
      EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("unaryEcho").build();
      StreamObserver<EchoOuterClass.EchoResponse> responseObserverIn = new StreamObserver<>(){

        @Override
        public void onNext(EchoOuterClass.EchoResponse echoResponse) {
          log.info("AsyncFacade unaryEcho response: {}", echoResponse.getMessage());
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {

        }
      };
      echoAsyncFacade.unaryEcho(echoRequest, responseObserverIn);

      return null;
    });
    HelloReply reply = stub.mockSayHelloToOther(req);

    HelloReply helloReply = HelloReply.newBuilder().setMessage("sayHello ==> " + reply.getMessage()).build();
    responseObserver.onNext(helloReply);
    responseObserver.onCompleted();
  }

  @Override
  public void mockSayHelloToOther(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
    log.info("mockSayHelloToOther {}", req.getName());

    HelloReply reply = HelloReply.newBuilder().setMessage("mockSayHelloToOther ==> " + req.getName()).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }
}
