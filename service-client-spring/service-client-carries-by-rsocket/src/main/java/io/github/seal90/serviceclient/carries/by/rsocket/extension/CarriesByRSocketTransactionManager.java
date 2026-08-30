package io.github.seal90.serviceclient.carries.by.rsocket.extension;

import io.github.seal90.serviceclient.carries.by.rsocket.context.*;
import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.MessageRSocket;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.AbstractReactiveTransactionManager;
import org.springframework.transaction.reactive.GenericReactiveTransaction;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import reactor.core.publisher.Mono;

@Slf4j
public class CarriesByRSocketTransactionManager extends AbstractReactiveTransactionManager {

    private String serviceName;
    private RSocketRequester rSocketRequester;
    private MessageRSocket messageRSocket;

    public CarriesByRSocketTransactionManager(String serviceName, RSocketRequester rSocketRequester, MessageRSocket messageRSocket) {
        this.serviceName = serviceName;
        this.rSocketRequester = rSocketRequester;
        this.messageRSocket = messageRSocket;
    }

    @Data
    public static class TransactionContext {
        private String transactionId;
    }

    @Override
    protected Object doGetTransaction(TransactionSynchronizationManager synchronizationManager) {

        return new TransactionContext();
    }

    private TransactionContext transaction(Object transaction) {
        return (TransactionContext)transaction;
    }

    @Override
    protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transaction, TransactionDefinition definition) {
        Transaction.BeginRequest request = Transaction.BeginRequest.newBuilder().build();
        synchronizationManager.bindResource(CarriesByRSocketTransactionManager.TransactionContext.class, transaction);

        Context.RpcRequest rpcRequest = Context.RpcRequest.newBuilder()
                .setTargetServiceName(serviceName)
                .setRoute("rsocket.msg.transaction.begin")
                .build();
        return rSocketRequester.route("message.carries.by.rsocket.requestResponse").metadata(metadataSpec -> {
            metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
        }).data(MessageRequest.of(request)).retrieveMono(MessageResponse.class).map(messageResponse -> {
            Transaction.BeginReplay replay = (Transaction.BeginReplay)messageResponse.getData(Transaction.BeginReplay.class);
            TransactionContext transactionContext = transaction(transaction);
            transactionContext.setTransactionId(replay.getId());
            return transactionContext;
        }).then();
    }

    @Override
    protected Mono<Void> doCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
        TransactionContext transactionContext = transaction(status.getTransaction());
        String transactionId = transactionContext.getTransactionId();

        Transaction.CommitRequest request = Transaction.CommitRequest.newBuilder().setId(transactionId).build();

        Context.RpcRequest rpcRequest = Context.RpcRequest.newBuilder()
                .setTargetServiceName(serviceName)
                .setRoute("rsocket.msg.transaction.commit")
                .build();
        return rSocketRequester.route("message.carries.by.rsocket.requestResponse").metadata(metadataSpec -> {
            metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
        }).data(MessageRequest.of(request)).retrieveMono(MessageResponse.class).map(messageResponse -> {
            Transaction.CommitReplay replay = (Transaction.CommitReplay)messageResponse.getData(Transaction.CommitReplay.class);
            return replay;
        }).then();
    }

    @Override
    protected Mono<Void> doRollback(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
        TransactionContext transactionContext = transaction(status.getTransaction());
        String transactionId = transactionContext.getTransactionId();

        Transaction.RollbackRequest request = Transaction.RollbackRequest.newBuilder().setId(transactionId).build();

        Context.RpcRequest rpcRequest = Context.RpcRequest.newBuilder()
                .setTargetServiceName(serviceName)
                .setRoute("rsocket.msg.transaction.rollback")
                .build();
        return rSocketRequester.route("message.carries.by.rsocket.requestResponse").metadata(metadataSpec -> {
            metadataSpec.metadata(rpcRequest, CarriesConstant.REQUEST_METADATA_MIMETYPE);
        }).data(MessageRequest.of(request)).retrieveMono(MessageResponse.class).map(messageResponse -> {
            Transaction.RollbackReplay replay = (Transaction.RollbackReplay)messageResponse.getData(Transaction.RollbackReplay.class);
            return replay;
        }).then();
    }
}
