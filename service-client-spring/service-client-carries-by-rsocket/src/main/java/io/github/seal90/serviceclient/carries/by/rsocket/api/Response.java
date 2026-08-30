package io.github.seal90.serviceclient.carries.by.rsocket.api;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Response<Data extends Message> {

    private static final String RESPONSE_CODE_METADATA_KEY = "response_code_metadata_key";

    private static final String RESPONSE_MESSAGE_METADATA_KEY = "response_message_metadata_key";

    private Data data;

    private Map<String, Any> metadata;

    private Response() {
    }

    private Response(ErrorCode errorCode) {
        this.metadata = new HashMap<>();
        metadata.put(RESPONSE_CODE_METADATA_KEY, Any.pack(StringValue.of(errorCode.getCode())));
        metadata.put(RESPONSE_MESSAGE_METADATA_KEY, Any.pack(StringValue.of(errorCode.getMessage())));
    }

    private Response(Map<String, Any> metadata, Data data) {
        this.metadata = new HashMap<>();
        if(metadata != null) {
            this.metadata.putAll(metadata);
        }
        this.data = data;
    }

    public static <Data extends Message> Response<Data> success(Data data) {
        Response<Data> response = new Response<>(ErrorCodeCore.SUCCESS);
        response.setData(data);
        return response;
    }

    public static <Data extends Message> Response<Data> error(ErrorCode errorCode) {
        return new Response<>(errorCode);
    }

    public static <Data extends Message> Response<Data> of(Map<String, Any> metadata, Data data) {
        return new Response(metadata, data);
    }

    public String getCode() {
        return getStringFromMetadata(RESPONSE_CODE_METADATA_KEY);
    }

    public String getMessage() {
        return getStringFromMetadata(RESPONSE_MESSAGE_METADATA_KEY);
    }

    public Boolean success() {
        return ErrorCodeCore.SUCCESS.getCode().equals(getCode());
    }

    public String getStringFromMetadata(String key) {
        return MetadataUtils.getStringFromMetadata(this.metadata, key);
    }

    public <T extends Message> T getValueFromMetadata(String key, Class<T> clazz) {
        return MetadataUtils.getValueFromMetadata(this.metadata, key, clazz);
    }
}


