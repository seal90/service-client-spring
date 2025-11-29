package io.github.seal90.serviceclient;

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

  /**
   * Http protocol implement by feign
   */
  public static final String HTTP_FEIGN = "HTTP_FEIGN";

}
