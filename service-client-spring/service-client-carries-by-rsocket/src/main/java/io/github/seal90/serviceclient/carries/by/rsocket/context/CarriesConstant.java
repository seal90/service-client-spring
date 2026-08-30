package io.github.seal90.serviceclient.carries.by.rsocket.context;

import org.springframework.util.MimeType;

public interface CarriesConstant {
    public static final String CONTEXT_REQUEST_METADATA_KEY = "context_request_metadata_key";
    public static final String CONTEXT_RESPONSE_METADATA_KEY = "context_response_metadata_key";

    public static final MimeType REQUEST_METADATA_MIMETYPE = MimeType.valueOf("application/rpc.request.metadata");
    public static final MimeType RESPONSE_METADATA_MIMETYPE = MimeType.valueOf("application/rpc.response.metadata");

    public static final String CONTEXT_TARGETSERVICENAME_ATTRIBUTE_KEY = "context_targetservicename_attribute_key";
    public static final String CONTEXT_ROUTE_ATTRIBUTE_KEY = "context_route_attribute_key";
    public static final String CONTEXT_METHOD_ATTRIBUTE_KEY = "context_method_attribute_key";

    public static final String CONTEXT_TRANSACTION_ID_METADATA_KEY = "context_transaction_id_metadata_key";
}
