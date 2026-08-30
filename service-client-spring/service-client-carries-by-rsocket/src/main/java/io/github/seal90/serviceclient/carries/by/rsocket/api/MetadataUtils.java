package io.github.seal90.serviceclient.carries.by.rsocket.api;

import com.google.protobuf.*;

import java.util.Map;

public class MetadataUtils {

    public static <T extends Message> T getValueFromMetadata(Map<String, Any> metadata, String key, Class<T> clazz) {
        Any data = metadata.get(key);
        if(data == null) {
            return null;
        }

        if (!data.is(clazz)) {
            throw new IllegalArgumentException(
                    String.format("Metadata key '%s' expects type %s, but actual type is %s",
                            key, clazz.getName(), data.getTypeUrl())
            );
        }

        try {
            return data.unpack(clazz);
        } catch (InvalidProtocolBufferException e) {
            // ignore
            throw new IllegalStateException("Failed to unpack metadata for key: " + key, e);
        }
    }

    public static String getStringFromMetadata(Map<String, Any> metadata, String key) {
        StringValue stringValue = MetadataUtils.getValueFromMetadata(metadata, key, StringValue.class);
        if(stringValue == null) {
            return null;
        }
        return stringValue.getValue();
    }

}
