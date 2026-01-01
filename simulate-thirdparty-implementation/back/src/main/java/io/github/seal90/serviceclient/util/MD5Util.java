package io.github.seal90.serviceclient.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {
  private static final String MD5_ALGORITHM_KEY = "MD5";

  public static String md5Hash(String input) {
    try {
      byte[] hash = MessageDigest.getInstance(MD5_ALGORITHM_KEY)
          .digest(input.getBytes(StandardCharsets.UTF_8));
      return asHexString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static String asHexString(byte[] messageDigest) {
    BigInteger number = new BigInteger(1, messageDigest);
    StringBuilder hexString = new StringBuilder(number.toString(16));

    while(hexString.length() < 32) {
      hexString.insert(0, '0');
    }

    return hexString.toString();
  }
}
