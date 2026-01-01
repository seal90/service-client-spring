package io.github.seal90.grpc_client;

import io.github.seal90.serviceclient.core.ServiceClient;
import io.github.seal90.serviceclient.proto.HelloReply;
import io.github.seal90.serviceclient.proto.HelloRequest;
import io.github.seal90.serviceclient.proto.HelloWorldServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class GrpcClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcClientApplication.class, args);
	}

	@ServiceClient(serviceName = "grpc-server")
	private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			HelloReply reply = stub.sayHello(HelloRequest.newBuilder().setName("ServiceClient").build());
			log.info("{}", reply);
			System.exit(0);
		};
	}

}
