package io.github.seal90.serviceclient.protocoltypefactory.grpc.extension.metadataforwarding;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class ForwardGrpcMetadataServerInterceptor implements ServerInterceptor {

  public static final Context.Key<Metadata> METADATA_CTX_KEY = Context.key(ForwardGrpcMetadataServerInterceptor.class.getName());

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
    Context newContext = Context.current().withValue(METADATA_CTX_KEY, metadata);
    return Contexts.interceptCall(newContext, serverCall, metadata, serverCallHandler);
  }
}
