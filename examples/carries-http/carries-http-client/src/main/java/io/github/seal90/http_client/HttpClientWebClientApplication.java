package io.github.seal90.http_client;

import io.github.seal90.http_facade.HelloRequest;
import io.github.seal90.http_facade.HelloReply;
import io.github.seal90.http_facade.HelloWorldFacade;
import io.github.seal90.serviceclient.core.ProtocolType;
import io.github.seal90.serviceclient.core.ServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class HttpClientWebClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(HttpClientWebClientApplication.class, args);
	}

	@ServiceClient(protocol = ProtocolType.CARRIES_HTTP, serviceName = "http-server-webclient")
	private HelloWorldFacade helloWorldFacade;

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			HelloRequest request = new HelloRequest();
			request.setName("ServiceClient");
			HelloReply reply = helloWorldFacade.sayHello(request).block();
			log.info("{}", reply);
			System.exit(0);
		};
	}

}
