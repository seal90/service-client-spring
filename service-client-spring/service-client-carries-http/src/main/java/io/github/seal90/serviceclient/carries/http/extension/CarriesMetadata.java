package io.github.seal90.serviceclient.carries.http.extension;

import lombok.Data;

import java.util.Map;

@Data
public class CarriesMetadata {

    public static final String CARRIES_METADATA_KEY = "carries_metadata";

    private Map<String, Object> forwardContext;

// remove?
    private Map<String, Object> protocolContext;

//    private HttpDownstreamContext httpDownstreamContext;
//
//    private HttpUpstreamContext httpUpstreamContext;
}
