package io.github.seal90.serviceclient.util;

public class ChannelNamePrefix {

  public static final String LB_PREFIX = "lb://";

  public static final String STATIC_PREFIX = "static://";

  public static final String DEFAULT_PREFIX = "default://";

  public static final String CHANNEL_PREFIX = "channel://";

  public static final String CONTEXT_PREFIX = "context://";

  public static String ofLb(String value) {
    return LB_PREFIX + value;
  }

  public static String ofStatic(String value) {
    return STATIC_PREFIX + value;
  }

  public static String ofDefault() {
    return DEFAULT_PREFIX;
  }

  public static String ofChannel(String value) {
    return CHANNEL_PREFIX + value;
  }

  public static String ofContext(String value) {
    return CONTEXT_PREFIX + value;
  }

}
