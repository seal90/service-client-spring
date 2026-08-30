package io.github.seal90.carries_server;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.github.seal90.serviceclient.carries.by.rsocket.context.CarriesConstant.CONTEXT_REQUEST_METADATA_KEY;

@Slf4j
@SpringBootApplication
public class CarriesServerApplication {

	@Autowired
	private ClientRegistry clientRegistry;

	public static void main(String[] args) {
		SpringApplication.run(CarriesServerApplication.class, args);
	}


	@Bean
	public CommandLineRunner mqConsumer() {
		return args -> {
			new Thread(() -> {
				Mqtt5BlockingClient mqtt5Client = MqttClient.builder()
						.identifier(UUID.randomUUID().toString())
						.serverHost("127.0.0.1")
						.useMqttVersion5()
						.buildBlocking();
				Mqtt5ConnAck mqtt5ConnAck = mqtt5Client.connect();
				try (final Mqtt5BlockingClient.Mqtt5Publishes publishes = mqtt5Client.publishes(MqttGlobalPublishFilter.ALL)) {
					mqtt5Client.subscribeWith().topicFilter("test/topic").qos(MqttQos.AT_LEAST_ONCE).send();
					while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Optional<Mqtt5Publish> msg = publishes.receive(1, TimeUnit.SECONDS);
							msg.ifPresent(publish -> {
								Mqtt5UserProperties mqtt5UserProperties = publish.getUserProperties();
								List<? extends Mqtt5UserProperty> mqtt5UserPropertiesList = mqtt5UserProperties.asList();
								for(Mqtt5UserProperty userProperty : mqtt5UserPropertiesList) {
									MqttUtf8String name = userProperty.getName();
									String nameString = StandardCharsets.UTF_8.decode(name.toByteBuffer()).toString();
//									MqttUtf8String value = userProperty.getValue();
//									String valueString = StandardCharsets.UTF_8.decode(value.toByteBuffer()).toString();
									log.info("name: {}", nameString);
								}
								Map<String, Any> metadata = mqtt5UserPropertiesList.stream().map(userProperty -> {
									MqttUtf8String name = userProperty.getName();
									String nameString = StandardCharsets.UTF_8.decode(name.toByteBuffer()).toString();
									MqttUtf8String value = userProperty.getValue();
									String valueString = StandardCharsets.UTF_8.decode(value.toByteBuffer()).toString();
									byte[] encoded = Base64.getDecoder().decode(valueString);
                                    try {
                                        Any any = Any.parseFrom(encoded);
										return Map.entry(nameString, any);
                                    } catch (InvalidProtocolBufferException e) {
                                        throw new RuntimeException(e);
                                    }
                                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
										(existing, replacement) -> existing));


//								publish.getPayloadAsBytes();
								Optional<ByteBuffer> byteBufferOptional = publish.getPayload();
								byteBufferOptional.ifPresent(byteBuffer -> {

									clientRegistry.sendMessage("message.carries.by.rsocket.mq", metadata, publish.getPayloadAsBytes());
//									String bodyString = StandardCharsets.UTF_8.decode(byteBuffer).toString();
//									log.info("bodyString is {}", bodyString);
								});
//								publish.acknowledge();
							});
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
				}
			}, "mqtt-subscriber-thread").start();
		};
	}

//	@Bean
//	public CommandLineRunner mqPublisher(Mqtt5BlockingClient mqtt5Client) {
//		return args -> {
//			new Thread(() -> {
//				while(true) {
//                    try {
//                        Thread.sleep(Duration.ofSeconds(5L));
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                    Mqtt5UserProperty mqtt5UserProperty = Mqtt5UserProperty.of("hello", "world");
//					Mqtt5UserProperties mqtt5UserProperties = Mqtt5UserProperties.of(mqtt5UserProperty);
//					mqtt5Client.publishWith().topic("test/topic").qos(MqttQos.AT_LEAST_ONCE).userProperties(mqtt5UserProperties).payload("1".getBytes()).send();
//				}
//			}).start();
//
//		};
//	}
}
