package io.github.seal90.carries_server;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.embedded.EmbeddedHiveMQBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.hivemq.HiveMQServer;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Configuration
public class HiveMqConfig {

    @Bean(destroyMethod = "stop")
    public EmbeddedHiveMQ hiveMQServer() throws Exception {
        EmbeddedHiveMQBuilder embeddedHiveMQBuilder = EmbeddedHiveMQ.builder()
                .withConfigurationFolder(Paths.get("examples/carries-by-rsocket/carries-server/src/main/resources/hivemq/config"))
                .withDataFolder(Paths.get("examples/carries-by-rsocket/carries-server/src/main/resources/hivemq/data"))
                .withExtensionsFolder(Paths.get("examples/carries-by-rsocket/carries-server/src/main/resources/hivemq/extensions"));

        final EmbeddedHiveMQ hiveMQ = embeddedHiveMQBuilder.build();
        hiveMQ.start().join();
        return hiveMQ;

//        HiveMQServer server = new HiveMQServer();
//        server.start();
//        return server;
    }

    @Bean
    public Mqtt5BlockingClient mqtt5Client(EmbeddedHiveMQ hiveMQServer) {
        Mqtt5BlockingClient client = MqttClient.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost("127.0.0.1")
                .useMqttVersion5()
                .buildBlocking();
        Mqtt5ConnAck mqtt5ConnAck = client.connect();
        return client;
    }


}
