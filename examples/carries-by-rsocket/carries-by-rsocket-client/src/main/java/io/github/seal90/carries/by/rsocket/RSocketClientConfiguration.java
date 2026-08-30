package io.github.seal90.carries.by.rsocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.WellKnownMimeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.MimeType;

@Slf4j
@Configuration
public class RSocketClientConfiguration {

    @Bean
    public RSocketRequester rSocketRequester(RSocketRequester.Builder rsocketRequesterBuilder,
                                             RSocketStrategies rsocketStrategies, MqCarriesByRSocketConsumer mqCarriesByRSocketConsumer) throws JsonProcessingException {

        SocketAcceptor responder = RSocketMessageHandler.responder(rsocketStrategies, mqCarriesByRSocketConsumer);
        log.info("register to server");
        RSocketRequester rsocketRequester = rsocketRequesterBuilder
                .setupRoute("register")
//                .setupData(data)
                .dataMimeType(MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()))
                .rsocketConnector(connector -> connector.acceptor(responder))
                .tcp("localhost",9898);
        rsocketRequester.rsocketClient().source().block();
        return rsocketRequester;
    }
}
