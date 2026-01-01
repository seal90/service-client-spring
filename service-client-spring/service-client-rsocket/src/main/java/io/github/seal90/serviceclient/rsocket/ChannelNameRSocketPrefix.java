package io.github.seal90.serviceclient.rsocket;

public class ChannelNameRSocketPrefix {

  public static final String REGISTRATION_PREFIX = "registration://";

  public static boolean isRegistration(String value) {
    return REGISTRATION_PREFIX.equals(value);
  }

  public static String withRegistration() {
    return REGISTRATION_PREFIX;
  }

}
