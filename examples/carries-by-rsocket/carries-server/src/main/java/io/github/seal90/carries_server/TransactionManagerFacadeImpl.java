package io.github.seal90.carries_server;

import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Transaction;
import io.github.seal90.serviceclient.carries.by.rsocket.extension.TransactionManagerFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Controller
public class TransactionManagerFacadeImpl implements TransactionManagerFacade {
    @Override
    public Mono<MessageResponse<Transaction.GetTransactionReplay>> doGetTransaction(Mono<MessageRequest<Transaction.GetTransactionRequest>> requestMono) {
        return null;
    }

    @Override
    public Mono<MessageResponse<Transaction.BeginReplay>> doBegin(Mono<MessageRequest<Transaction.BeginRequest>> requestMono) {

        return requestMono.map(messageRequest -> {
            String id = UUID.randomUUID().toString();
            log.info("Begin transaction id: {}", id);
            return MessageResponse.success(Transaction.BeginReplay.newBuilder().setId(id).build());
        });
    }

    @Override
    public Mono<MessageResponse<Transaction.CommitReplay>> doCommit(Mono<MessageRequest<Transaction.CommitRequest>> requestMono) {
        return requestMono.map(messageRequest -> {
            Transaction.CommitRequest request = messageRequest.getData(Transaction.CommitRequest.class);
            log.info("Commit transaction id: {}", request.getId());
            return MessageResponse.success(Transaction.CommitReplay.newBuilder().build());
        });
    }

    @Override
    public Mono<MessageResponse<Transaction.RollbackReplay>> doRollback(Mono<MessageRequest<Transaction.RollbackRequest>> requestMono) {
        return requestMono.map(messageRequest -> {
            Transaction.RollbackRequest request = messageRequest.getData(Transaction.RollbackRequest.class);
            log.info("Rollback transaction id: {}", request.getId());
            return MessageResponse.success(Transaction.RollbackReplay.newBuilder().build());
        });
    }
}
