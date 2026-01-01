package io.github.seal90.serviceclient.rsocket.protocoltypefactory.client;

import lombok.Getter;
import org.springframework.messaging.rsocket.RSocketRequester;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ClientContext {
    private final String serviceId;
    private final String instanceId;
    private final RSocketRequester requester;
    private final Map<String, String> tags = new HashMap<>();

    public ClientContext(String serviceId, String instanceId, RSocketRequester requester, Map<String, String> tags) {
        this.serviceId = serviceId;
        this.instanceId = instanceId;
        this.requester = requester;
        if(tags != null) {
            this.tags.putAll(tags);
        }
    }

}