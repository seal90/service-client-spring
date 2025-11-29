package io.github.seal90.grpc_server;

import io.github.seal90.serviceclient.ServiceClient;
import io.github.seal90.serviceclient.proto.HelloReply;
import io.github.seal90.serviceclient.proto.HelloRequest;
import io.github.seal90.serviceclient.proto.HelloWorldServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HelloWorldService extends HelloWorldServiceGrpc.HelloWorldServiceImplBase {

  @ServiceClient(serviceName = "HelloWorldService")
  private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;

  @Override
  public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
    log.info("sayHello {}", req.getName());

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
