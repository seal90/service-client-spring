package io.github.seal90.mqtt_moquette;

import io.moquette.broker.Server;
import io.moquette.broker.config.ClasspathResourceLoader;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MqttMoquetteConfiguration {

    @Bean(destroyMethod = "stopServer")
    public Server moquetteBroker() throws IOException {
        Server server = new Server();

        IResourceLoader resourceLoader = new ClasspathResourceLoader("moquette.conf");
        ResourceLoaderConfig config = new ResourceLoaderConfig(resourceLoader);
        config.setProperty("persistence_enabled", "false");
        server.startServer(config);
        return server;
    }
}