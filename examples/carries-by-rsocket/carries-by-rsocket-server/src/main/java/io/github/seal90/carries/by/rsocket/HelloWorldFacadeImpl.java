package io.github.seal90.carries.by.rsocket;

import com.google.protobuf.Any;
import io.github.seal90.carries.by.rsocket.facade.HelloReply;
import io.github.seal90.carries.by.rsocket.facade.HelloRequest;
import io.github.seal90.carries.by.rsocket.facade.HelloWorldFacade;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Map;

@Controller
public class HelloWorldFacadeImpl implements HelloWorldFacade {

    private Integer i = 0;

    @Override
    public HelloReply sayHello(HelloRequest request) {
        return HelloReply.newBuilder().setMessage("hello ==> " + request.getName()).build();
    }

    @Override
    public Flux<HelloReply> sayHello(Mono<HelloRequest> request) {

        return request.flatMapMany(helloRequest -> {
            return Flux.range(1, 10).map(index ->
                    HelloReply.newBuilder().setMessage(helloRequest.getName() + index).build());
//            if (i++ % 3 == 0) {
//                return Flux.<HelloReply>empty().contextWrite(context -> {
//                    Map<String, Any> bizContext = context.get("app_context");
//                    return context;
//                });
//            } else {
//                return Flux.range(1, 10).map(index ->
//                        HelloReply.newBuilder().setMessage(helloRequest.getName() + index).build());
//            }
        });
    }

}
