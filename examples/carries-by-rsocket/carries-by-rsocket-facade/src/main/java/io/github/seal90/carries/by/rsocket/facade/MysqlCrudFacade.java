package io.github.seal90.carries.by.rsocket.facade;

import com.mysql.cj.x.protobuf.MysqlxCrud;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Mono;

public interface MysqlCrudFacade {

    @MessageMapping("message.carries.by.rsocket.db.insert")
    default Mono<MessageResponse> insert(Mono<MessageRequest<MysqlxCrud.Insert>> requestMono) {
        return null;
    }
}
