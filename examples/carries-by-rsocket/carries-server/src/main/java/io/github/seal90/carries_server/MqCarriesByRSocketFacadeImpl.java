package io.github.seal90.carries_server;

import com.google.protobuf.Any;
import com.hivemq.client.internal.mqtt.datatypes.MqttUtf8StringImpl;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import io.github.seal90.carries.by.rsocket.facade.HelloReply;
import io.github.seal90.carries.by.rsocket.facade.HelloRequest;
import io.github.seal90.carries.by.rsocket.facade.MqCarriesByRSocketFacade;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.rsocket.service.RSocketExchange;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class MqCarriesByRSocketFacadeImpl implements MqCarriesByRSocketFacade {

    @Autowired
    private Mqtt5BlockingClient mqtt5Client;

    @Override
    public Mono<MessageResponse<HelloReply>> mq(Mono<MessageRequest<HelloRequest>> requestMono) {

        return requestMono.map(request -> {
            Map<String, Any> metadata = request.getMetadata();
            Any data = request.getData();


            List<Mqtt5UserProperty> propertyList = metadata.entrySet().stream().map(m -> {
                String encoded = Base64.getEncoder()
                        .encodeToString(m.getValue().toByteArray());
                return Mqtt5UserProperty.of(MqttUtf8String.of(m.getKey()), MqttUtf8StringImpl.of(encoded));
            }).collect(Collectors.toList());
//            Mqtt5UserProperty mqtt5UserProperty = Mqtt5UserProperty.of("hello", "world");
            Mqtt5UserProperties mqtt5UserProperties = Mqtt5UserProperties.of(propertyList);
            mqtt5Client.publishWith().topic("test/topic").qos(MqttQos.AT_LEAST_ONCE)
                    .userProperties(mqtt5UserProperties)
                    .payload(request.toByteArray()).send();

            return MessageResponse.success(HelloReply.newBuilder().build());
        });
    }
}
