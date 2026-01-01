package io.github.seal90.mqtt_eclipse_client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seal90.serviceclient.core.ServiceClient;
import io.github.seal90.serviceclient.mqtt.eclipse.ProtocolTypeMqttEclipse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

@Slf4j
@SpringBootApplication
public class MqttEclipseClientApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context =SpringApplication.run(MqttEclipseClientApplication.class, args);

		Runtime.getRuntime().addShutdownHook(new Thread(context::close));

		synchronized (MqttEclipseClientApplication.class) {
			while (context.isRunning()) {
				try {
					MqttEclipseClientApplication.class.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
	}

	@ServiceClient(protocol = ProtocolTypeMqttEclipse.MQTT_ECLIPSE, serviceName = "mqtt-moquette-server")
	private MessageChannel messageChannel;

	@Autowired
	private MessageChannel mqttOutboundMessageChannel;

	@Bean
	public CommandLineRunner runner() {

		return args -> {
			int num = 0;
			while (num < 10 ) {
				HelloData helloData = new HelloData();
				helloData.setMessage("ServiceClient"+ num++);
				Message<HelloData> message = MessageBuilder.withPayload(helloData).build();
				boolean sendSuccess = messageChannel.send(message);
				log.info("serviceclient send message {}, success flag {}", message, sendSuccess);

				// It might be due to some misconfiguration that prevents sending custom objects.
				ObjectMapper objectMapper = new ObjectMapper();
				String messageData = objectMapper.writeValueAsString(helloData);
				Message<String> stringMessage = MessageBuilder.withPayload(messageData).build();
				mqttOutboundMessageChannel.send(stringMessage);
				log.info("spring send message {}, success flag {}", message, sendSuccess);
				Thread.sleep(10000);
			}
		};
	}

}
