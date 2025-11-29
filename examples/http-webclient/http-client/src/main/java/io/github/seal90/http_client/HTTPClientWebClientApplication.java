package io.github.seal90.http_client;

import io.github.seal90.http_facade.HelloRequest;
import io.github.seal90.http_facade.HelloWorldFacade;
import io.github.seal90.serviceclient.ProtocolType;
import io.github.seal90.serviceclient.ServiceClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HTTPClientWebClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(HTTPClientWebClientApplication.class, args);
	}

	@ServiceClient(protocol = ProtocolType.HTTP, serviceName = "httpServer", channelName = "static://http://127.0.0.1:8080")
	private HelloWorldFacade helloWorldFacade;

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			HelloRequest request = new HelloRequest();
			request.setName("ServiceClient");
			System.out.println(helloWorldFacade.sayHello(request).block());
			System.exit(0);
		};
	}

}
