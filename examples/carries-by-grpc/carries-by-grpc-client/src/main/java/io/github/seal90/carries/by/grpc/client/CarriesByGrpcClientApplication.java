package io.github.seal90.carries.by.grpc.client;

import com.google.common.util.concurrent.ListenableFuture;
import grpc.examples.echo.EchoGrpc;
import grpc.examples.echo.EchoOuterClass;
import io.github.seal90.carries.by.rsocket.facade.EchoAsyncFacade;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.github.seal90.serviceclient.proto.HelloReply;
import io.github.seal90.serviceclient.proto.HelloRequest;
import io.github.seal90.serviceclient.proto.HelloWorldServiceGrpc;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.BlockingClientCall;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@SpringBootApplication
public class CarriesByGrpcClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(CarriesByGrpcClientApplication.class, args);
	}

	@ServiceClient(serviceName = "hello-world")
	private HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub;

	@ServiceClient(serviceName = "hello-world")
	private EchoGrpc.EchoStub echoStub;

	@ServiceClient(serviceName = "hello-world")
	private EchoGrpc.EchoBlockingStub echoBlockingStub;

	@ServiceClient(serviceName = "hello-world")
	private EchoGrpc.EchoBlockingV2Stub echoBlockingV2Stub;

	@ServiceClient(serviceName = "hello-world")
	private EchoGrpc.EchoFutureStub echoFutureStub;

	@ServiceClient(protocol = "CARRIES_BY_GRPC", serviceName = "hello-world")
	private EchoAsyncFacade echoAsyncFacade;



	@Bean
	public CommandLineRunner runner() {
		return args -> {
			sayHelloCall();

			unaryEchoAsyncFacadeCall();

			unaryEchoCall();
			serverStreamingEchoCall();
			clientStreamingEchoCall();
			bidirectionalStreamingEchoCall();

			unaryEchoBlockingV2Call();
			serverStreamingEchoBlockingV2Call();
			clientStreamingEchoBlockingV2Call();
			bidirectionalStreamingEchoBlockingV2Call();

			unaryEchoFutureCall();

			Thread.sleep(1000);
			System.exit(0);
		};
	}

	private void sayHelloCall() {
		Metadata metadata = new Metadata();
		metadata.put(Metadata.Key.of("x-custom-header", Metadata.ASCII_STRING_MARSHALLER), "custom-value");

		HelloReply reply = stub.sayHello(HelloRequest.newBuilder().setName("ServiceClient").build());
		log.info("reply: {}", reply);
	}

	private void unaryEchoAsyncFacadeCall() {
		EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("unaryEcho").build();
		StreamObserver<EchoOuterClass.EchoResponse> responseObserver = new StreamObserver<>(){

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
		echoAsyncFacade.unaryEcho(echoRequest, responseObserver);
	}

	private void unaryEchoCall() {
		EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("unaryEcho").build();
		StreamObserver<EchoOuterClass.EchoResponse> responseObserver = new StreamObserver<>(){

			@Override
			public void onNext(EchoOuterClass.EchoResponse echoResponse) {
				log.info("stub unaryEcho response: {}", echoResponse.getMessage());
			}

			@Override
			public void onError(Throwable throwable) {

			}

			@Override
			public void onCompleted() {

			}
		};
		echoStub.unaryEcho(echoRequest, responseObserver);
	}

	private void serverStreamingEchoCall() {
		EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("serverStreamingEcho").build();
		StreamObserver<EchoOuterClass.EchoResponse> responseObserver = new StreamObserver<>(){

			@Override
			public void onNext(EchoOuterClass.EchoResponse echoResponse) {
				log.info("stub serverStreamingEcho response: {}", echoResponse.getMessage());
			}

			@Override
			public void onError(Throwable throwable) {

			}

			@Override
			public void onCompleted() {

			}
		};
		echoStub.serverStreamingEcho(echoRequest, responseObserver);
	}

	private void clientStreamingEchoCall() {
		StreamObserver<EchoOuterClass.EchoResponse> responseObserver = new StreamObserver<>(){

			@Override
			public void onNext(EchoOuterClass.EchoResponse echoResponse) {
				log.info("stub clientStreamingEcho response: {}", echoResponse.getMessage());
			}

			@Override
			public void onError(Throwable throwable) {

			}

			@Override
			public void onCompleted() {

			}
		};
		StreamObserver<EchoOuterClass.EchoRequest> requestObserver = echoStub.clientStreamingEcho(responseObserver);
		EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("clientStreamingEcho").build();
		requestObserver.onNext(echoRequest);
		requestObserver.onNext(echoRequest);
		requestObserver.onCompleted();
	}

	private void bidirectionalStreamingEchoCall() {
		StreamObserver<EchoOuterClass.EchoResponse> responseObserver = new StreamObserver<>(){

			@Override
			public void onNext(EchoOuterClass.EchoResponse echoResponse) {
				log.info("stub bidirectionalStreamingEcho response: {}", echoResponse.getMessage());
			}

			@Override
			public void onError(Throwable throwable) {

			}

			@Override
			public void onCompleted() {

			}
		};
		StreamObserver<EchoOuterClass.EchoRequest> requestObserver = echoStub.bidirectionalStreamingEcho(responseObserver);
		EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("bidirectionalStreamingEcho").build();
		requestObserver.onNext(echoRequest);
		requestObserver.onNext(echoRequest);
		requestObserver.onCompleted();
	}

	private void unaryEchoBlockingV2Call() {

        try {
			EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("unaryEcho").build();
			EchoOuterClass.EchoResponse echoResponse =  echoBlockingV2Stub.unaryEcho(echoRequest);
			log.info("BlockingV2 unaryEcho response: {}", echoResponse.getMessage());
        } catch (StatusException e) {
			Status status = e.getStatus();
			Metadata trailers = e.getTrailers();
			Set<String> keys = trailers.keys();
			for(String key : keys) {
				String value = trailers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
				log.info("unaryEchoBlockingV2Call {}: {}", key, value);
			}
//            throw new RuntimeException(e);
        }
	}

	private void serverStreamingEchoBlockingV2Call() {
		EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("serverStreamingEcho").build();
		BlockingClientCall<?, EchoOuterClass.EchoResponse> echoResponseBlockingClientCall = echoBlockingV2Stub.serverStreamingEcho(echoRequest);
        try {
			while(echoResponseBlockingClientCall.hasNext()) {
				EchoOuterClass.EchoResponse echoResponse = echoResponseBlockingClientCall.read();
				log.info("BlockingV2 serverStreamingEcho response: {}", echoResponse.getMessage());
			}
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (StatusException e) {
            throw new RuntimeException(e);
        }
	}

	private void clientStreamingEchoBlockingV2Call() {
		try {
			BlockingClientCall<EchoOuterClass.EchoRequest, EchoOuterClass.EchoResponse> blockingClientCall = echoBlockingV2Stub.clientStreamingEcho();

			for(int i=0;i<3;i++) {
				EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("clientStreamingEcho"+i).build();
				blockingClientCall.write(echoRequest);
			}
			blockingClientCall.halfClose();
			EchoOuterClass.EchoResponse echoResponse = blockingClientCall.read();

			log.info("BlockingV2 clientStreamingEcho response: {}", echoResponse.getMessage());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (StatusException e) {
			throw new RuntimeException(e);
		}
	}

	private void bidirectionalStreamingEchoBlockingV2Call() {

		BlockingClientCall<EchoOuterClass.EchoRequest, EchoOuterClass.EchoResponse> blockingClientCall = echoBlockingV2Stub.bidirectionalStreamingEcho();
		Thread writer = new Thread(()->{
			try {
				for(int i=0;i<3;i++) {
					EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("clientStreamingEcho"+i).build();
					blockingClientCall.write(echoRequest);
				}
				blockingClientCall.halfClose();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (StatusException e) {
				throw new RuntimeException(e);
			}
		});

		Thread reader = new Thread(()->{
			try {
				while(blockingClientCall.hasNext()) {
					EchoOuterClass.EchoResponse echoResponse = blockingClientCall.read();
					log.info("BlockingV2 bidirectionalStreamingEcho response: {}", echoResponse.getMessage());
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (StatusException e) {
				throw new RuntimeException(e);
			}
		});

		writer.start();
		reader.start();
        try {
			writer.join();
			reader.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

	private void unaryEchoFutureCall() {

		try {
			EchoOuterClass.EchoRequest echoRequest = EchoOuterClass.EchoRequest.newBuilder().setMessage("unaryEcho").build();
			ListenableFuture<EchoOuterClass.EchoResponse> echoResponseListenableFuture = echoFutureStub.unaryEcho(echoRequest);
			EchoOuterClass.EchoResponse echoResponse = echoResponseListenableFuture.get();
			log.info("Future unaryEcho response: {}", echoResponse.getMessage());
		} catch (ExecutionException e) {
			if(e.getCause() instanceof StatusRuntimeException es) {
				Status status = es.getStatus();
				Metadata trailers = es.getTrailers();
				Set<String> keys = trailers.keys();
				for(String key : keys) {
					String value = trailers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
					log.info("unaryEchoFutureCall {}: {}", key, value);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
