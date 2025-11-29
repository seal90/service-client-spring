package io.github.seal90.serviceclient;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ServiceClient
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceClient {

  /**
   * protocol
   * @return protocol
   */
  String protocol() default "";

  /**
   * serviceName
   * @return serviceName
   */
  String serviceName();

  /**
   * channelName
   * @return channelName
   */
  String channelName() default "";

  /**
   * interceptors
   * @return Interceptors related to protocol
   */
  String[] interceptors() default {};

}
