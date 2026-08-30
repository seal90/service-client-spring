package io.github.seal90.carries_server;

import com.google.protobuf.Any;
import io.github.seal90.serviceclient.carries.by.rsocket.context.CarriesConstant;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class ClientRegistry {

    private RSocketRequester requester;

    @ConnectMapping("register")
    public void onConnect(RSocketRequester requester) {
        this.requester = requester;

        requester.rsocket()
                .onClose()
                .doFinally(sig -> {
                    this.requester = null;
                })
                .subscribe();
    }

    public void sendMessage(String route, Map<String, Any> metadata, Object data) {
        Context.RpcRequest rpcRequest = Context.RpcRequest.newBuilder().putAllMetadata(metadata).build();
        requester.route(route).metadata(metadataSpec->{
            metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
        }).data(data).retrieveMono(DataBuffer.class).block();
    }
}
