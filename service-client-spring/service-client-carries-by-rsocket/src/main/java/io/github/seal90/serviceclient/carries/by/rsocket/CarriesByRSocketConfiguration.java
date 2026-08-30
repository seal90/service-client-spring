package io.github.seal90.serviceclient.carries.by.rsocket;

import com.google.protobuf.Message;
import io.github.seal90.serviceclient.carries.by.rsocket.context.CarriesConstant;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Context;
import io.github.seal90.serviceclient.carries.by.rsocket.extension.CustomProtobufEncoder;
import io.github.seal90.serviceclient.carries.by.rsocket.properties.CarriesByRSocketProperties;
import io.rsocket.metadata.WellKnownMimeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeType;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CarriesByRSocketProperties.class})
public class CarriesByRSocketConfiguration {

    @Bean
    public CarriesByRSocketProtocolTypeFactory carriesByRSocketProtocolTypeFactory(ApplicationContext applicationContext,
            Environment environment, CarriesByRSocketProperties carriesByRSocketProperties, RSocketRequester.Builder rsocketRequesterBuilder) {
        return new CarriesByRSocketProtocolTypeFactory(applicationContext, environment, carriesByRSocketProperties, rsocketRequesterBuilder);
    }

    @Bean
    public CarriesByRSocketProtocolTypeTransactionFactory carriesByRSocketProtocolTypeTransactionFactory(ApplicationContext applicationContext,
                                                                                                         Environment environment, CarriesByRSocketProperties carriesByRSocketProperties, RSocketRequester.Builder rsocketRequesterBuilder) {
        return new CarriesByRSocketProtocolTypeTransactionFactory(applicationContext, environment, carriesByRSocketProperties, rsocketRequesterBuilder);
    }

    @Bean
    public RSocketStrategiesCustomizer protobufRSocketStrategies() {
        ProtobufDecoder protobufDecoder = new ProtobufDecoder();
        return (strategy) -> {
            strategy.encoder(new ProtobufEncoder())
                    .decoder(new ProtobufDecoder())
                    .encoder(new CustomProtobufEncoder(CarriesConstant.REQUEST_METADATA_MIMETYPE, CarriesConstant.RESPONSE_METADATA_MIMETYPE))
                    .metadataExtractorRegistry(registry -> {
//                        registry.metadataToExtract(CarriesConstant.REQUEST_METADATA_MIMETYPE, Context.RpcRequest.class, CarriesConstant.CONTEXT_REQUEST_METADATA_KEY);
//                        registry.metadataToExtract(CarriesConstant.RESPONSE_METADATA_MIMETYPE, Context.RpcResponse.class, CarriesConstant.CONTEXT_RESPONSE_METADATA_KEY);
                        registry.metadataToExtract(CarriesConstant.REQUEST_METADATA_MIMETYPE, DataBuffer.class, (dataBuffer, metadata) -> {
                            ResolvableType resolvableType = ResolvableType.forClass(Context.RpcRequest.class);
                            Message data = protobufDecoder.decode(dataBuffer, resolvableType, MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()), null);
                            metadata.put(CarriesConstant.CONTEXT_REQUEST_METADATA_KEY, data);
                        });
                        registry.metadataToExtract(CarriesConstant.RESPONSE_METADATA_MIMETYPE, DataBuffer.class, (dataBuffer, metadata) -> {
                            ResolvableType resolvableType = ResolvableType.forClass(Context.RpcResponse.class);
                            Message data = protobufDecoder.decode(dataBuffer, resolvableType, MimeType.valueOf(WellKnownMimeType.APPLICATION_PROTOBUF.getString()), null);
                            metadata.put(CarriesConstant.CONTEXT_RESPONSE_METADATA_KEY, data);
                        });
                    });
        };
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("to notify that I can handle invocations");
    }

    @EventListener(ContextClosedEvent.class)
    public void close() {
        log.info("to notify that I can't handle invocations");
    }
}
