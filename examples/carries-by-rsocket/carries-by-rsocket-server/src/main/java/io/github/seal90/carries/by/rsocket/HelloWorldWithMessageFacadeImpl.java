package io.github.seal90.carries.by.rsocket;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import io.github.seal90.carries.by.rsocket.facade.HelloReply;
import io.github.seal90.carries.by.rsocket.facade.HelloRequest;
import io.github.seal90.carries.by.rsocket.facade.HelloWorldWithMessageFacade;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class HelloWorldWithMessageFacadeImpl implements HelloWorldWithMessageFacade {

    public Flux<Context.Response> sayHelloAllFlux(Flux<Context.Request> request) {
        return request.map(r -> {
            Any any = r.getData();
            try {
                HelloRequest helloRequest = any.unpack(HelloRequest.class);
                HelloReply helloReply = HelloReply.newBuilder().setMessage("hello: "+ helloRequest.getName()).build();
                Context.Response response = Context.Response.newBuilder().putMetadata("hello", Any.pack(StringValue.newBuilder().setValue("world").build())).setData(Any.pack(helloReply)).build();
                return response;
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
