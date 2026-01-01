package io.github.seal90.rsocket_server.client;

import io.github.seal90.serviceclient.rsocket.protocoltypefactory.client.RSocketClientRegistryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.util.function.Tuple2;

@Slf4j
@Controller
public class ClientRegistry {

    @Autowired
    private RSocketClientRegistryContext rSocketClientRegistryContext;

    @ConnectMapping("register")
    public void onConnect(@Payload String data, RSocketRequester requester) {

        Tuple2<String, String> registryInfo = rSocketClientRegistryContext.registry(data, requester);
        requester.rsocket()
            .onClose()
            .doFinally(sig -> {
                rSocketClientRegistryContext.remove(registryInfo);
            })
            .subscribe();

        String serviceId = registryInfo.getT1();
        String instanceId = registryInfo.getT2();
        log.info("Client connected: {}/{}", serviceId, instanceId);
    }

}