package io.github.seal90.serviceclient.carries.by.rsocket.extension;

import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import io.github.seal90.serviceclient.carries.by.rsocket.context.Transaction;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Mono;

public interface TransactionManagerFacade {

    @MessageMapping("rsocket.msg.transaction.getTransaction")
    public Mono<MessageResponse<Transaction.GetTransactionReplay>> doGetTransaction(Mono<MessageRequest<Transaction.GetTransactionRequest>> requestMono);

    @MessageMapping("rsocket.msg.transaction.begin")
    public Mono<MessageResponse<Transaction.BeginReplay>> doBegin(Mono<MessageRequest<Transaction.BeginRequest>> requestMono);

    @MessageMapping("rsocket.msg.transaction.commit")
    public Mono<MessageResponse<Transaction.CommitReplay>> doCommit(Mono<MessageRequest<Transaction.CommitRequest>> requestMono);

    @MessageMapping("rsocket.msg.transaction.rollback")
    public Mono<MessageResponse<Transaction.RollbackReplay>> doRollback(Mono<MessageRequest<Transaction.RollbackRequest>> requestMono);

}
