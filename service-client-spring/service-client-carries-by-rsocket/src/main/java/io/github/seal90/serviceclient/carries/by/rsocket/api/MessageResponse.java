//package io.github.seal90.serviceclient.carries.by.rsocket.api;
//
//import com.google.protobuf.Any;
//import com.google.protobuf.Message;
//import com.google.protobuf.StringValue;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Getter
//@Setter
//public class MessageResponse<Data extends Message> extends
//        com.google.protobuf.GeneratedMessage implements MessageResponseOrBuilder{
//
//    private static final String RESPONSE_CODE_METADATA_KEY = "response_code_metadata_key";
//
//    private static final String RESPONSE_MESSAGE_METADATA_KEY = "response_message_metadata_key";
//
//    private Data data;
//
//    private Map<String, Any> metadata;
//
//    private MessageResponse() {
//    }
//
//    private MessageResponse(ErrorCode errorCode) {
//        this.metadata = new HashMap<>();
//        metadata.put(RESPONSE_CODE_METADATA_KEY, Any.pack(StringValue.of(errorCode.getCode())));
//        metadata.put(RESPONSE_MESSAGE_METADATA_KEY, Any.pack(StringValue.of(errorCode.getMessage())));
//    }
//
//    private MessageResponse(Map<String, Any> metadata, Data data) {
//        this.metadata = new HashMap<>();
//        if(metadata != null) {
//            this.metadata.putAll(metadata);
//        }
//        this.data = data;
//    }
//
//    public static <Data extends Message> MessageResponse<Data> success(Data data) {
//        MessageResponse<Data> response = new MessageResponse<>(ErrorCodeCore.SUCCESS);
//        response.setData(data);
//        return response;
//    }
//
//    public static <Data extends Message> MessageResponse<Data> error(ErrorCode errorCode) {
//        return new MessageResponse<>(errorCode);
//    }
//
//    public static <Data extends Message> MessageResponse<Data> of(Map<String, Any> metadata, Data data) {
//        return new MessageResponse(metadata, data);
//    }
//
//    public String getCode() {
//        return getStringFromMetadata(RESPONSE_CODE_METADATA_KEY);
//    }
//
//    public String getMessage() {
//        return getStringFromMetadata(RESPONSE_MESSAGE_METADATA_KEY);
//    }
//
//    public Boolean success() {
//        return ErrorCodeCore.SUCCESS.getCode().equals(getCode());
//    }
//
//    public String getStringFromMetadata(String key) {
//        return MetadataUtils.getStringFromMetadata(this.metadata, key);
//    }
//
//    public <T extends Message> T getValueFromMetadata(String key, Class<T> clazz) {
//        return MetadataUtils.getValueFromMetadata(this.metadata, key, clazz);
//    }
//
//    private static com.google.protobuf.Descriptors.FileDescriptor
//            descriptor;
//    private static final com.google.protobuf.Descriptors.Descriptor
//            internal_static_Response_descriptor;
//    private static final
//    com.google.protobuf.GeneratedMessage.FieldAccessorTable
//            internal_static_Response_fieldAccessorTable;
//    static {
//        descriptor = com.google.protobuf.Descriptors.FileDescriptor
//                .internalBuildGeneratedFileFrom(descriptorData,
//                        new com.google.protobuf.Descriptors.FileDescriptor[] {
//                                com.google.protobuf.AnyProto.getDescriptor(),
//                        });
//
//        internal_static_Response_descriptor =
//                getDescriptor().getMessageTypes().get(4);
//
//        internal_static_Response_fieldAccessorTable = new
//                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
//                internal_static_Response_descriptor,
//                new java.lang.String[] { "Metadata", "Data", });
//    }
//
//    public static com.google.protobuf.Descriptors.FileDescriptor
//    getDescriptor() {
//        return descriptor;
//    }
//
//    @Override
//    protected FieldAccessorTable internalGetFieldAccessorTable() {
//        return internal_static_Response_fieldAccessorTable
//                .ensureFieldAccessorsInitialized(
//                        io.github.seal90.serviceclient.carries.by.rsocket.context.Context.Response.class, io.github.seal90.serviceclient.carries.by.rsocket.context.Context.Response.Builder.class);
//        return null;
//    }
//
//    @Override
//    public Message.Builder newBuilderForType() {
//        return null;
//    }
//
//    @Override
//    public Message.Builder toBuilder() {
//        return null;
//    }
//
//    @Override
//    public Message getDefaultInstanceForType() {
//        return null;
//    }
//}
