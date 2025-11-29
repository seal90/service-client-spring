package io.github.seal90.grpc_client;

import io.github.seal90.serviceclient.proto.HelloRequest;
import io.github.seal90.serviceclient.proto.HelloWorldServiceGrpc;
import io.github.seal90.serviceclient.ServiceClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GrpcClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcClientApplication.class, args);
	}

	@ServiceClient(serviceName = "HelloWorldService")
	private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			System.out.println(stub.sayHello(HelloRequest.newBuilder().setName("ServiceClient").build()));
			System.exit(0);
		};
	}

}
