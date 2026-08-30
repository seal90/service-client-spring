package io.github.seal90.carries_server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seal90.serviceclient.carries.by.rsocket.context.CarriesConstant;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import io.rsocket.metadata.WellKnownMimeType;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.util.MimeType;

@Configuration
public class ServerRSocketConfig {

//    @Bean
//    public RSocketStrategiesCustomizer httpProxyHeader(ObjectMapper objectMapper) {
//        return (strategy) -> {
//            strategy.encoder(new ProtobufEncoder())
//                    .decoder(new ProtobufDecoder())
//                    .metadataExtractorRegistry(registry -> {
//                registry.metadataToExtract(MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()), Context.RpcRequest.class, CarriesConstant.CONTEXT_REQUEST_METADATA_KEY);
//            });
//        };
//    }

}
