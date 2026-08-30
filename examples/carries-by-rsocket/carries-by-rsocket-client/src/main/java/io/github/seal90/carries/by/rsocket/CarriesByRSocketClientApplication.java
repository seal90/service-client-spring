package io.github.seal90.carries.by.rsocket;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.mysql.cj.x.protobuf.Mysqlx;
import com.mysql.cj.x.protobuf.MysqlxCrud;
import io.github.seal90.carries.by.rsocket.facade.*;
import io.github.seal90.serviceclient.carries.by.rsocket.api.Request;
import io.github.seal90.serviceclient.carries.by.rsocket.context.CarriesConstant;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.rsocket.metadata.WellKnownMimeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootApplication
public class CarriesByRSocketClientApplication {

    @ServiceClient(protocol="CARRIES_BY_RSOCKET", serviceName = "carries-by-rsocket-server")
    private HelloWorldFacade helloWorldFacade;

    @ServiceClient(protocol="CARRIES_BY_RSOCKET", serviceName = "carries-by-rsocket-server")
    private HelloWorldWithBoxFacade helloWorldWithBoxFacade;

    @ServiceClient(protocol="CARRIES_BY_RSOCKET", serviceName = "carries-by-rsocket-server")
    private HelloWorldWithMetadataFacade helloWorldWithMetadataFacade;

    @ServiceClient(protocol="CARRIES_BY_RSOCKET", serviceName = "carries-by-rsocket-server")
    private HelloWorldWithMessageFacade helloWorldWithMessageFacade;

    @ServiceClient(protocol="CARRIES_BY_RSOCKET", serviceName = "carries-by-rsocket-server")
    private HelloWorldWithMessageBoxFacade helloWorldWithMessageBoxFacade;

    @ServiceClient(protocol="CARRIES_BY_RSOCKET", serviceName = "mq-carries-by-rsocket")
    private MqCarriesByRSocketFacade mqCarriesByRSocketFacade;

    @ServiceClient(protocol="CARRIES_BY_RSOCKET", serviceName = "mysql-crud-by-rsocket")
    private MysqlCrudFacade mysqlCrudFacade;

    @ServiceClient(protocol = "CARRIES_BY_RSOCKET_TRANSACTION", serviceName = "mysql-crud-by-rsocket")
    private TransactionalOperator transactionalOperator;

    @Autowired
    private RSocketRequester.Builder rSocketRequesterBuilder;

    public static void main(String[] args) {
        SpringApplication.run(CarriesByRSocketClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner() {
        return args -> {
//            helloWorldFacadeCall();
//            helloWorldWithMetadataFacadeCall();
//            helloWorldWithBoxFacadeCall();
//            helloWorldWithMessageFacadeCall();

//            helloWorldWithMessageBoxFacadeCall();
//            System.exit(0);

//            mqCarriesByRSocketFacadeCall();

            mysqlCrudFacadeCall();
        };
    }

    public void test() {
        RSocketRequester rSocketRequester = rSocketRequesterBuilder
                .dataMimeType(MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()))
                .tcp("localhost",20001);
        HelloRequest request = HelloRequest.newBuilder().setName("ServiceClient").build();
        Context.RpcRequest rpcRequest = Context.RpcRequest.newBuilder().build();
        rSocketRequester.route("rsocket.msg.requestStream.sayHello").metadata(metadataSpec->{
            metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
        }).data(Mono.just(request)).retrieveFlux(HelloReply.class).map(helloReply -> {
            log.info("{}", helloReply);
            return helloReply;
        }).blockLast();
    }

    private void helloWorldFacadeCall() {
        HelloRequest request = HelloRequest.newBuilder().setName("ServiceClient").build();
        HelloReply reply = helloWorldFacade.sayHello(request);
        log.info("{}", reply);

        Flux<HelloReply> replyFlux = helloWorldFacade.sayHello(Mono.just(request));
        replyFlux.map(helloReply -> {
            log.info("{}", helloReply);
            return helloReply;
        }).blockLast();
    }

    private void helloWorldWithMetadataFacadeCall() {
        HelloRequest request = HelloRequest.newBuilder().setName("ServiceClient").build();
        HelloReply reply = helloWorldWithMetadataFacade.sayHello(request);
        log.info("{}", reply);

        // Pass metadata
        Context.RpcMetadata rpcMetadata = Context.RpcMetadata.newBuilder()
                .setKey("hello")
                .setValue(Any.pack(StringValue.of("world"))).build();
        HelloRequest requestWithMetadata = HelloRequest.newBuilder().setName("ServiceClient with metadata").build();
        HelloReply replyWithMetadata = helloWorldWithMetadataFacade.sayHello(requestWithMetadata, rpcMetadata);
        log.info("{}", replyWithMetadata);
    }

    private void helloWorldWithBoxFacadeCall() {
        Flux<Request<HelloRequest>> request = Flux.range(1, 10).map(i-> {
            HelloRequest helloRequest = HelloRequest.newBuilder().setName("name" +i).build();
            Request<HelloRequest> r = Request.of(helloRequest);
            return r;
        });
        helloWorldWithBoxFacade.sayHelloAllFlux(request).map(response -> {
            log.info("response: {}", response.getData().getMessage());
            return response;
        }).blockLast();
    }

    private void helloWorldWithMessageFacadeCall() {
        Map<String, Any> metadataMap = new HashMap<>();

        Flux<Context.Request> request = Flux.range(1,5).index().map(tuple -> {
            long index = tuple.getT1();
            HelloRequest helloRequest = HelloRequest.newBuilder().setName("hello name " + index).build();
            Context.Request r;
            if(index == 0) {
                r = Context.Request.newBuilder().setData(Any.pack(helloRequest)).putAllMetadata(metadataMap).build();
            } else {
                r = Context.Request.newBuilder().setData(Any.pack(helloRequest)).build();
            }
            return r;
        });
        Flux<Context.Response> responseFlux = helloWorldWithMessageFacade.sayHelloAllFlux(request);

        responseFlux.map(response -> {
            Any anyData = response.getData();
            try {
                HelloReply helloReply = anyData.unpack(HelloReply.class);
                log.info("replay: {}", helloReply);
                return helloReply;
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }).blockLast();
    }

    private void helloWorldWithMessageBoxFacadeCall() {

        HelloRequest helloRequestSayHello = HelloRequest.newBuilder().setName("").build();
        MessageRequest<HelloRequest> messageRequestSayHello = MessageRequest.of(helloRequestSayHello);
        Mono<MessageResponse<HelloReply>> messageResponseMono = helloWorldWithMessageBoxFacade.sayHello(Mono.just(messageRequestSayHello));
        messageResponseMono.map(messageResponse -> {
            HelloReply helloReply = messageResponse.getData(HelloReply.class);
            log.info("box hello replay: {}", helloReply.getMessage());
            return messageResponse;
        }).block();

        Flux<MessageRequest<HelloRequest>> request = Flux.range(1, 10).map(num -> {
            HelloRequest helloRequest = HelloRequest.newBuilder().setName("hello message box" + num).build();
            MessageRequest<HelloRequest> messageRequest = MessageRequest.of(helloRequest);
            return messageRequest;
        });
        Flux<MessageResponse<HelloReply>> responseFlux = helloWorldWithMessageBoxFacade.sayHelloAllFlux(request);
        responseFlux.map(messageResponse -> {
            HelloReply helloReply = messageResponse.getData(HelloReply.class);
            String message = helloReply.getMessage();
            log.info("message box replay message: {}", message);
            return messageResponse;
        }).blockLast();
    }

    private void mqCarriesByRSocketFacadeCall() {
        new Thread(() -> {
            int i = 0;
            while (true) {
                try {
                    Thread.sleep(Duration.ofSeconds(5L));
                    Map<String, Any> metadata = new HashMap<>();
                    metadata.put("metadata_test_key", Any.pack(StringValue.of("metadata_test_value")));

                    HelloRequest helloRequest = HelloRequest.newBuilder().setName("mq test" + i++).build();
                    MessageRequest<HelloRequest> request = MessageRequest.of(metadata, helloRequest);
                    Mono<MessageRequest<HelloRequest>> requestMono = Mono.just(request);
                    Mono<MessageResponse<HelloReply>> responseMono = mqCarriesByRSocketFacade.mq(requestMono);
                    responseMono.block();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

    }

    private void mysqlCrudFacadeCall() {
        transactionalOperator.execute((status) -> {
            MysqlxCrud.Collection collection = MysqlxCrud.Collection.newBuilder().setName("hello").build();
            MysqlxCrud.Insert insert = MysqlxCrud.Insert.newBuilder().setCollection(collection).build();
            Mono<MessageRequest<MysqlxCrud.Insert>> requestMono = Mono.just(MessageRequest.of(insert));
            return mysqlCrudFacade.insert(requestMono);
        }).blockLast();
    }
}
