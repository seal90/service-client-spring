package io.github.seal90.serviceclient.carries.by.rsocket.api;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Request<Data extends Message> {

    private Data data;

    private Map<String, Any> metadata;

    private Map<String, Object> attributes;

    private Request(Map<String, Any> metadata, Data data) {
        this.attributes = new HashMap<>();
        this.metadata = new HashMap<>();
        if(metadata != null) {
            this.metadata.putAll(metadata);
        }
        this.data = data;
    }

    public static <Data extends Message> Request<Data> of() {
        return new Request<>(null, null);
    }

    public static <Data extends Message> Request<Data> of(Map<String, Any> metadata) {
        return new Request<>(metadata, null);
    }

    public static <Data extends Message> Request<Data> of(Data data) {
        return new Request<>(null, data);
    }

    public static <Data extends Message> Request<Data> of(Map<String, Any> metadata, Data data) {
        return new Request<>(metadata, data);
    }

    public <T extends Message> T getValueFromMetadata(String key, Class<T> clazz) {
        return MetadataUtils.getValueFromMetadata(this.metadata, key, clazz);
    }

    public String getStringFromMetadata(String key) {
        return MetadataUtils.getStringFromMetadata(this.metadata, key);
    }
}
