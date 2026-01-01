package io.github.seal90.serviceclient.core;

/**
 * Protocol client type
 */
public interface ProtocolType {

  /**
   * Grpc protocol implement by spring grpc
   */
  public static final String GRPC = "GRPC";

  /**
   * Http protocol implement by WebClient or RestTemplate
   */
  public static final String HTTP = "HTTP";


  public static final String RSOCKET = "RSOCKET";

  /**
   * MQTT protocol implement by spring-integration
   */
  public static final String MQTT = "MQTT";

}
