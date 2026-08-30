package io.github.seal90.carries.by.grpc.server;

import io.github.seal90.serviceclient.carries.by.grpc.transaction.CarriesByGrpcTransactionGrpc;
import io.github.seal90.serviceclient.carries.by.grpc.transaction.Transaction;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class CarriesByGrpcTransactionService extends CarriesByGrpcTransactionGrpc.CarriesByGrpcTransactionImplBase {

    public void doBegin(Transaction.BeginRequest request,
                         StreamObserver<Transaction.BeginReplay> responseObserver) {
        String id = UUID.randomUUID().toString();
        log.info("doBegin id: {}", id);
        Transaction.BeginReplay beginReplay = Transaction.BeginReplay.newBuilder().setId(id).build();
        responseObserver.onNext(beginReplay);
        responseObserver.onCompleted();
    }

    public void doCommit(Transaction.CommitRequest request,
                          StreamObserver<Transaction.CommitReplay> responseObserver) {
        String id = request.getId();
        log.info("doCommit id: {}", id);
        Transaction.CommitReplay commitReplay = Transaction.CommitReplay.newBuilder().build();
        responseObserver.onNext(commitReplay);
        responseObserver.onCompleted();
    }

    public void doRollback(Transaction.RollbackRequest request,
                            StreamObserver<Transaction.RollbackReplay> responseObserver) {
        String id = request.getId();
        log.info("doRollback id: {}", id);
        Transaction.RollbackReplay rollbackReplay = Transaction.RollbackReplay.newBuilder().build();
        responseObserver.onNext(rollbackReplay);
        responseObserver.onCompleted();

    }
}
