package io.github.seal90.serviceclient.carries.http.extension;

import lombok.Data;

import java.util.Map;

@Data
public class HttpUpstreamContext {
    public static final String CarriesMetadata_KEY = "http_upstream_context";

    private int code;
    private Map<String, String> headers;
}
