package io.github.seal90.rsocket_client;

import io.github.seal90.rsocket_facade.HelloRSocketClient2ServerFacade;
import io.github.seal90.rsocket_facade.HelloReply;
import io.github.seal90.rsocket_facade.HelloRequest;
import io.github.seal90.rsocket_facade.HelloWorldFacade;
import io.github.seal90.serviceclient.core.ProtocolType;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.netty.util.ResourceLeakDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.service.RSocketServiceProxyFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@SpringBootApplication
public class RSocketClientApplication {

	static {
		// Enables paranoid resource leak detection which reports where the leaked object was accessed recently,
		// at the cost of the highest possible overhead (for testing purposes only).
		// -Dio.netty.leakDetection.level=paranoid
		ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
	}

	public static void main(String[] args) {
		SpringApplication.run(RSocketClientApplication.class, args);
	}

	@ServiceClient(protocol = ProtocolType.RSOCKET, serviceName = "rsocket-server")
	private HelloWorldFacade helloWorldFacade;

	@ServiceClient(protocol = ProtocolType.RSOCKET, serviceName = "rsocket-server")
	private HelloRSocketClient2ServerFacade client2ServerFacade;

	@Autowired
	private RSocketRequester rSocketRequester;

	@Bean
	public CommandLineRunner runner() {
		return args -> {

			HelloRequest request = new HelloRequest();
			request.setName("ServiceClient");

			Mono<HelloReply> helloReplyMono = helloWorldFacade.sayHello(request);

			// Demonstrates a scenario of making requests directly using RSocketRequester.
			Flux<HelloReply> replyDirectly = rSocketRequester.route("client2Server.requestChannel").data(Flux.just(request)).retrieveFlux(HelloReply.class);

			// Demonstrates a scenario of using a custom RSocketRequester and creating a client class.
			RSocketServiceProxyFactory factory = RSocketServiceProxyFactory.builder().rsocketRequester(rSocketRequester).build();
			HelloRSocketClient2ServerFacade facade = factory.createClient(HelloRSocketClient2ServerFacade.class);
			Flux<HelloReply> replyByClient = facade.requestChannel(Flux.just(request));

			// Demonstrates the use of a ServiceClient.
			// client(by HelloRSocketClient2ServerFacade)
			// -> server HelloRSocketClient2ServerFacade (by HelloRSocketServer2ClientFacade)
			// -> client HelloRSocketServer2ClientFacade
			Mono<Void> fireAndForget = client2ServerFacade.fireAndForget(request);
			Mono<HelloReply> reqResp = client2ServerFacade.requestResponse(request);
			Flux<HelloReply> stream = client2ServerFacade.requestStream(request);
			Flux<HelloReply> channel = client2ServerFacade.requestChannel(Flux.just(request));

			Mono<Void> allDone = Mono.when(
					helloReplyMono.doOnNext(replay -> log.info("command replay sayHello {}", replay)).then(),
					replyDirectly.doOnNext(replay -> log.info("command replay directly requestChannel {}", replay)).then(),
					replyByClient.doOnNext(replay -> log.info("command replay by client requestChannel {}", replay)).then(),
					fireAndForget,
					reqResp.doOnNext(replay -> log.info("command replay requestResponse {}", replay)).then(),
					stream.doOnNext(replay -> log.info("command replay requestStream {}", replay)).then(),
					channel.doOnNext(replay -> log.info("command replay requestChannel {}", replay)).then()
			).doOnTerminate(() -> log.info("All RSocket operations completed."));
			allDone.block();

			System.exit(0);
		};
	}

}
