package io.github.seal90.serviceclient.carries.by.rsocket.extension;

import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

public class CustomProtobufEncoder extends ProtobufEncoder {

    private final MimeType[] customMimeTypes;

    public CustomProtobufEncoder(MimeType... customMimeTypes) {
        this.customMimeTypes = customMimeTypes;
    }

    @Override
    protected boolean supportsMimeType(@Nullable MimeType mimeType) {
        if (mimeType == null) {
            return true;
        }
        for (MimeType m : customMimeTypes) {
            if (m.isCompatibleWith(mimeType)) {
                return true;
            }
        }
        return false;
    }
}
