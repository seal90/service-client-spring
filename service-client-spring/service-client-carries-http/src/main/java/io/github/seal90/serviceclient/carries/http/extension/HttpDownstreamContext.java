package io.github.seal90.serviceclient.carries.http.extension;

import lombok.Data;

import java.util.Map;

@Data
public class HttpDownstreamContext {

    public static final String CarriesMetadata_KEY = "http_downstream_context";

    private HttpExchangeProperties httpExchange;

    private Map<String, Object> forwardedContext;
}
