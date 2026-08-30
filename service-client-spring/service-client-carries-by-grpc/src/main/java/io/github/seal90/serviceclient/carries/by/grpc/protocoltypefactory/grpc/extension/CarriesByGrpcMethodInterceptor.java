package io.github.seal90.serviceclient.carries.by.grpc.protocoltypefactory.grpc.extension;

import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.AbstractStub;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import static io.grpc.MethodDescriptor.generateFullMethodName;

public class CarriesByGrpcMethodInterceptor implements MethodInterceptor {

    private String serviceName;

    private AbstractStub stub;

    public CarriesByGrpcMethodInterceptor(String serviceName, AbstractStub stub) {
        this.serviceName = serviceName;
        this.stub = stub;
    }

    @Override
    public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
        Method invocationMethod = invocation.getMethod();
        Method stubMethod = stub.getClass().getMethod(invocationMethod.getName(), invocationMethod.getParameterTypes());
        return stubMethod.invoke(stub, invocation.getArguments());

//        Method targetMethod = invocation.getMethod();
//        targetMethod.getParameterTypes();
//        Object[] arguments = invocation.getArguments();
//
//        targetMethod.getParameterTypes()
//        MethodDescriptor.newBuilder()
//                .setType(MethodDescriptor.MethodType.UNARY)
//                .setFullMethodName(generateFullMethodName("", "UnaryEcho"))
//                .setSampledToLocalTracing(true)
//                .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
//                        grpc.examples.echo.EchoOuterClass.EchoRequest.getDefaultInstance()))
//                .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
//                        grpc.examples.echo.EchoOuterClass.EchoResponse.getDefaultInstance()))
//                .setSchemaDescriptor(new EchoMethodDescriptorSupplier("UnaryEcho"))
//                .build();
//
//        // EchoMethodDescriptorSupplier
//
//        io.grpc.stub.ClientCalls.asyncUnaryCall(
//                stub.getChannel().newCall(getUnaryEchoMethod(), getCallOptions()), arguments[0], arguments[1]);
    }
}
