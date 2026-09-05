package io.github.seal90.serviceclient.carries.by.grpc.protocoltypefactory.grpc.extension;

import io.github.seal90.serviceclient.carries.by.grpc.transaction.CarriesByGrpcTransactionGrpc;
import io.github.seal90.serviceclient.carries.by.grpc.transaction.Transaction;
import io.grpc.Context;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@Slf4j
public class CarriesByGrpcTransactionManager extends AbstractPlatformTransactionManager {

    public static final Context.Key<TransactionContext> TRANSACTION_CONTEXT = Context.key(TransactionContext.class.getName());

    private String serviceName;

    private CarriesByGrpcTransactionGrpc.CarriesByGrpcTransactionBlockingStub carriesByGrpcTransaction;

    public CarriesByGrpcTransactionManager(String serviceName, CarriesByGrpcTransactionGrpc.CarriesByGrpcTransactionBlockingStub carriesByGrpcTransaction) {
        this.serviceName = serviceName;
        this.carriesByGrpcTransaction = carriesByGrpcTransaction;
    }

    @Override
    protected Object doGetTransaction() throws TransactionException {
        TransactionContext transactionContext = new TransactionContext();
        Context context = Context.current().withValue(TRANSACTION_CONTEXT, transactionContext).attach();
        transactionContext.setContext(context);
        return transactionContext;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        TransactionContext transactionContext = transaction(transaction);

        Transaction.BeginRequest beginRequest = Transaction.BeginRequest.newBuilder().build();
        Transaction.BeginReplay beginReplay = carriesByGrpcTransaction.doBegin(beginRequest);
        transactionContext.setTransactionId(beginReplay.getId());
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        TransactionContext transactionContext = transaction(status.getTransaction());
        try {
            String transactionId = transactionContext.getTransactionId();

            Transaction.CommitRequest commitRequest = Transaction.CommitRequest.newBuilder().setId(transactionId).build();
            carriesByGrpcTransaction.doCommit(commitRequest);
        } finally {
            Context context = transactionContext.getContext();
            Context.current().detach(context);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        TransactionContext transactionContext = transaction(status.getTransaction());
        try {
            String transactionId = transactionContext.getTransactionId();

            Transaction.RollbackRequest rollbackRequest = Transaction.RollbackRequest.newBuilder().setId(transactionId).build();
            carriesByGrpcTransaction.doRollback(rollbackRequest);
        } finally {
            Context context = transactionContext.getContext();
            Context.current().detach(context);
        }

    }

    @Data
    public static class TransactionContext {
        private String transactionId;
        private Context context;
    }

    private TransactionContext transaction(Object transaction) {
        return (TransactionContext)transaction;
    }
}
