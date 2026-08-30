package io.github.seal90.carries.by.rsocket.facade;

import com.mysql.cj.x.protobuf.MysqlxCrud;
import org.springframework.messaging.handler.annotation.MessageMapping;

public interface MysqlCrudFacade {

    @MessageMapping("message.carries.by.rsocket.db.insert")
    default void insert(MysqlxCrud.Insert insert) {

    }
}
