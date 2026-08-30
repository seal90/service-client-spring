package io.github.seal90.serviceclient.carries.http.extension;

import lombok.Data;

@Data
public class HttpExchangeProperties {

    private String serviceName;

    private String url;

    private String method;

    private String contentType;

    private String[] accept;

    private String[] headers;
}
