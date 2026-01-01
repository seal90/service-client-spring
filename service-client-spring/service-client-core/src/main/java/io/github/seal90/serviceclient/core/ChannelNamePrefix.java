package io.github.seal90.serviceclient.core;

public final class ChannelNamePrefix {

  public static final String CONTEXT_PREFIX = "context://";
  public static final String STATIC_PREFIX = "static://";
  public static final String CHANNEL_PREFIX = "channel://";
  public static final String DEFAULT_PREFIX = "default://";
  public static final String LB_PREFIX = "lb://";

  public static boolean isContext(String value) {
    return value != null && value.startsWith(CONTEXT_PREFIX);
  }

  public static boolean isStatic(String value) {
    return value != null && value.startsWith(STATIC_PREFIX);
  }

  public static boolean isChannel(String value) {
    return value != null && value.startsWith(CHANNEL_PREFIX);
  }

  public static boolean isDefault(String value) {
    return DEFAULT_PREFIX.equals(value);
  }

  public static boolean isLb(String value) {
    return value != null && value.startsWith(LB_PREFIX);
  }

  public static String extractContext(String value) {
    return value.substring(CONTEXT_PREFIX.length());
  }

  public static String extractStatic(String value) {
    return value.substring(STATIC_PREFIX.length());
  }

  public static String extractChannel(String value) {
    return value.substring(CHANNEL_PREFIX.length());
  }

  public static String extractLb(String value) {
    return value.substring(LB_PREFIX.length());
  }

  public static String withContext(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Value must not be null for context prefix");
    }
    return CONTEXT_PREFIX + value;
  }

  public static String withStatic(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Value must not be null for static prefix");
    }
    return STATIC_PREFIX + value;
  }

  public static String withChannel(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Value must not be null for channel prefix");
    }
    return CHANNEL_PREFIX + value;
  }

  public static String withDefault() {
    return DEFAULT_PREFIX;
  }

  public static String withLb(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Value must not be null for lb prefix");
    }
    return LB_PREFIX + value;
  }

  public static String extractValue(String prefixedValue) {
    if (prefixedValue == null) {
      return null;
    }
    if (isLb(prefixedValue)) {
      return prefixedValue.substring(LB_PREFIX.length());
    } else if (isStatic(prefixedValue)) {
      return prefixedValue.substring(STATIC_PREFIX.length());
    } else if (isChannel(prefixedValue)) {
      return prefixedValue.substring(CHANNEL_PREFIX.length());
    } else if (isContext(prefixedValue)) {
      return prefixedValue.substring(CONTEXT_PREFIX.length());
    } else if (isDefault(prefixedValue)) {
      return "";
    }
    return null; // unknown prefix
  }
}