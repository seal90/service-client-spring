package io.github.seal90.carries.by.grpc.client;

import io.github.seal90.serviceclient.proto.HelloReply;
import io.github.seal90.serviceclient.proto.HelloRequest;
import io.github.seal90.serviceclient.proto.HelloWorldServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringGrpcClientRun {

    @Configuration
    @ImportGrpcClients(target="hello-world", value= HelloWorldServiceGrpc.HelloWorldServiceBlockingStub.class)
    public static class SpringGrpcClientConfig {

    }

    @Autowired
    public HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub;

    public void helloWorld() {
        HelloRequest request = HelloRequest.newBuilder().setName("").build();
        HelloReply reply = blockingStub.sayHello(request);
        log.info("replay {}", reply);
    }

}
