package io.github.seal90.carries.by.rsocket;

import io.github.seal90.carries.by.rsocket.facade.HelloReply;
import io.github.seal90.carries.by.rsocket.facade.HelloRequest;
import io.github.seal90.carries.by.rsocket.facade.HelloWorldWithBoxFacade;
import io.github.seal90.serviceclient.carries.by.rsocket.api.Request;
import io.github.seal90.serviceclient.carries.by.rsocket.api.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
public class HelloWorldWithBoxFacadeImpl implements HelloWorldWithBoxFacade {

    @Override
    public Response<HelloReply> sayHello(Request<HelloRequest> request) {
        return null;
    }

    @Override
    public Flux<Response<HelloReply>> sayHelloFlux(Mono<Request<HelloRequest>> request) {
        return null;
    }

    @Override
    public Flux<Response<HelloReply>> sayHelloAllFlux(Flux<Request<HelloRequest>> request) {
        return request.index().map(t -> {
            long num = t.getT1();
            Request<HelloRequest> r = t.getT2();
            if(num == 0) {
                log.info("metadata: {}", r.getMetadata());
            }
            log.info("data: {}", r.getData());

            return Response.success(HelloReply.newBuilder().setMessage("hello "+num).build());
        });
    }

}
