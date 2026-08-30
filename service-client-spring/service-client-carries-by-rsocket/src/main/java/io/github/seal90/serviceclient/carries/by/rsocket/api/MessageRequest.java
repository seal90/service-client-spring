//package io.github.seal90.serviceclient.carries.by.rsocket.api;
//
//import com.google.protobuf.Any;
//import com.google.protobuf.Message;
//import lombok.Getter;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Getter
//public class MessageRequest<Data extends Message> extends
//        com.google.protobuf.GeneratedMessage implements MessageRequestOrBuilder {
//
//    private Data data;
//
//    private Map<String, Any> metadata;
//
//    private Map<String, Object> attributes;
//
//    private MessageRequest(Map<String, Any> metadata, Data data) {
//        this.attributes = new HashMap<>();
//        this.metadata = new HashMap<>();
//        if(metadata != null) {
//            this.metadata.putAll(metadata);
//        }
//        this.data = data;
//    }
//
//    public static <Data extends Message> MessageRequest<Data> of() {
//        return new MessageRequest<>(null, null);
//    }
//
//    public static <Data extends Message> MessageRequest<Data> of(Map<String, Any> metadata) {
//        return new MessageRequest<>(metadata, null);
//    }
//
//    public static <Data extends Message> MessageRequest<Data> of(Data data) {
//        return new MessageRequest<>(null, data);
//    }
//
//    public static <Data extends Message> MessageRequest<Data> of(Map<String, Any> metadata, Data data) {
//        return new MessageRequest<>(metadata, data);
//    }
//
//    public <T extends Message> T getValueFromMetadata(String key, Class<T> clazz) {
//        return MetadataUtils.getValueFromMetadata(this.metadata, key, clazz);
//    }
//
//    public String getStringFromMetadata(String key) {
//        return MetadataUtils.getStringFromMetadata(this.metadata, key);
//    }
//
//    @Override
//    protected FieldAccessorTable internalGetFieldAccessorTable() {
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