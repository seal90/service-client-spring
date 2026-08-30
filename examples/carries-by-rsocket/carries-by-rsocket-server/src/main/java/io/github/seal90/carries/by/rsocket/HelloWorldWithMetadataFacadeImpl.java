package io.github.seal90.carries.by.rsocket;

import io.github.seal90.carries.by.rsocket.facade.HelloReply;
import io.github.seal90.carries.by.rsocket.facade.HelloRequest;
import io.github.seal90.carries.by.rsocket.facade.HelloWorldWithMetadataFacade;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import org.springframework.stereotype.Controller;

@Controller
public class HelloWorldWithMetadataFacadeImpl implements HelloWorldWithMetadataFacade {

    @Override
    public HelloReply sayHello(HelloRequest request, Context.RpcMetadata... rpcMetadata) {
        // How can I get user information from metadata?
        // Using reactive code makes the code more complex.
        // Although ThreadLocal can be safely used within this method, it is difficult to seamlessly integrate with the external reactive framework due to asynchronous context propagation.
        // For non-reactive scenarios, should we fall back to ThreadLocal, utilizing annotations to store and retrieve data within the context? threadlocal <-> context
        // get user info, get mq tag, put user info, put timeout...
        Context.RpcMetadata respMetadata = Context.RpcMetadata.newBuilder().build();
        return HelloReply.newBuilder().setMessage("hello ==> " + request.getName()).build();
    }

}
