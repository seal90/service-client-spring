package io.github.seal90.carries.by.rsocket;

import com.google.protobuf.Any;
import io.github.seal90.carries.by.rsocket.facade.HelloReply;
import io.github.seal90.carries.by.rsocket.facade.HelloRequest;
import io.github.seal90.carries.by.rsocket.facade.HelloWorldWithMessageBoxFacade;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class HelloWorldWithMessageBoxFacadeImpl implements HelloWorldWithMessageBoxFacade {

    @Override
    public Mono<MessageResponse<HelloReply>> sayHello(Mono<MessageRequest<HelloRequest>> request) {

        return request.map(messageRequest -> {
            // TODO InvocableHandlerMethod Consider capturing the type information upon deserialization and storing it inside the object,
            //  so that getData() can later be called without a type argument.
            Any data = messageRequest.getData();
            HelloRequest helloRequest = messageRequest.getData(HelloRequest.class);

            HelloReply helloReply = HelloReply.newBuilder().setMessage("hello box: " + helloRequest.getName()).build();
            MessageResponse<HelloReply> messageResponse = MessageResponse.success(helloReply);
            return messageResponse;
        });

    }

    @Override
    public Flux<MessageResponse<HelloReply>> sayHelloAllFlux(Flux<MessageRequest<HelloRequest>> request) {
        return request.map(messageRequest -> {
            HelloRequest helloRequest = messageRequest.getData(HelloRequest.class);
            String name = helloRequest.getName();

            HelloReply helloReply = HelloReply.newBuilder().setMessage("message box server receive: " + name).build();
            MessageResponse<HelloReply> messageResponse = MessageResponse.success(helloReply);
            return messageResponse;
        });
    }

}
