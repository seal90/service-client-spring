package io.github.seal90.carries_server;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import com.mysql.cj.x.protobuf.MysqlxCrud;
import io.github.seal90.carries.by.rsocket.facade.MysqlCrudFacade;
import io.github.seal90.serviceclient.carries.by.rsocket.context.CarriesConstant;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
public class MysqlCrudFacadeImpl implements MysqlCrudFacade {

    @Override
    public Mono<MessageResponse> insert(Mono<MessageRequest<MysqlxCrud.Insert>> requestMono) {

        return requestMono.map(messageRequest -> {
            StringValue transactionIdStringValue = messageRequest.getMetadata(CarriesConstant.CONTEXT_TRANSACTION_ID_METADATA_KEY, StringValue.class);
            if(transactionIdStringValue != null) {
                String transactionId = transactionIdStringValue.getValue();
                log.info("transactionId is: {}", transactionId);
            }
            return MessageResponse.success(null);
        });
    }

}
