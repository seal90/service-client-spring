package io.github.seal90.serviceclient;

import java.lang.reflect.Member;

/**
 * ServiceClient protocol type creator
 */
public interface ProtocolTypeFactory {

  /**
   * Create protocol client
   * @param injectionTarget injection target
   * @param injectionType injection type
   * @param annotation ServiceClient
   * @return channel
   * @param <T> channel type
   */
  <T> T create(final Member injectionTarget, final Class<T> injectionType, final ServiceClient annotation);

  /**
   * Support protocol
   * @return Support protocol name
   */
  String supportProtocol();
}
