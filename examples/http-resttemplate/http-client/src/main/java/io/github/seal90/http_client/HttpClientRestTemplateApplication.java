package io.github.seal90.http_client;

import io.github.seal90.http_facade.HelloReply;
import io.github.seal90.http_facade.HelloRequest;
import io.github.seal90.http_facade.HelloWorldFacade;
import io.github.seal90.http_facade.HelloWorldFacade2;
import io.github.seal90.serviceclient.core.ProtocolType;
import io.github.seal90.serviceclient.core.ServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class HttpClientRestTemplateApplication {

	public static void main(String[] args) {
		SpringApplication.run(HttpClientRestTemplateApplication.class, args);
	}

	@ServiceClient(protocol = ProtocolType.HTTP, serviceName = "http-server-resttemplate")
	private HelloWorldFacade helloWorldFacade;

	// nonsupport
	@ServiceClient(protocol = ProtocolType.HTTP, serviceName = "http-server-resttemplate")
	private HelloWorldFacade2 helloWorldFacade2;

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			HelloRequest request = new HelloRequest();
			request.setName("ServiceClient");
			HelloReply reply = helloWorldFacade.sayHello(request);
			log.info("{}", reply);
			System.exit(0);
		};
	}

}
