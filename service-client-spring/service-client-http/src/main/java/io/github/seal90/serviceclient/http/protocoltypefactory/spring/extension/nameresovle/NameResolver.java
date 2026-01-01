//package io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.nameresovle;
//
//import io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.properties.ProtocolTypeHttpChannelProperties;
//import io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.properties.ProtocolTypeHttpProperties;
//import io.github.seal90.serviceclient.http.protocoltypefactory.spring.extension.properties.ProtocolTypeHttpServiceProperties;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//
//import java.util.List;
//import java.util.Random;
//
//public class NameResolver {
//
//  @Data
//  @AllArgsConstructor
//  public static class Result {
//    private Boolean nameResolved;
//    private String resolvedAddress;
//  }
//
//  public static Result resolve(String serviceName, String channelName, ProtocolTypeHttpProperties protocolTypeProperties) {
//    boolean nameResolved = true;
//    String resolvedAddress = null;
//
//    if("".equals(channelName)) {
//      resolvedAddress = resolveByServiceName(serviceName, channelName, protocolTypeProperties);
//      nameResolved = resolvedAddress != null;
//    } else {
//      if (channelName.startsWith("static://")) {
//        resolvedAddress = channelName.replaceFirst("static://", "");
//      } else if (channelName.startsWith("default://")) {
//        resolvedAddress = parseDefaultChannel(protocolTypeProperties);
//      } else if (channelName.startsWith("lb://")) {
//        resolvedAddress = "http://" + channelName.replaceFirst("lb://", "");
//        nameResolved = false;
//      } else if (channelName.startsWith("channel://")) {
//        resolvedAddress = parseByChannelName(channelName.replaceFirst("channel://", ""), protocolTypeProperties);
//      } else {
//        resolvedAddress = parseByChannelName(channelName, protocolTypeProperties);
//      }
//    }
//
//    return new Result(nameResolved, resolvedAddress);
//  }
//
//  private static String resolveByServiceName(String serviceName, String channelName, ProtocolTypeHttpProperties protocolTypeProperties) {
//    String parsedAddress = null;
//    ProtocolTypeHttpServiceProperties serviceProperties = protocolTypeProperties.getServices().get(serviceName);
//    if(serviceProperties != null) {
//      String resolvedChannelName = serviceProperties.getChannelName();
//      if(resolvedChannelName != null) {
//        parsedAddress = parseByChannelName(resolvedChannelName, protocolTypeProperties);
//        if(parsedAddress == null) {
//          throw new RuntimeException("Parsed "+serviceName+" by channelName: " + resolvedChannelName + " fail.");
//        }
//      } else {
//        ProtocolTypeHttpChannelProperties channelProperties = serviceProperties.getChannelConfig();
//        parsedAddress = parseChannel(channelProperties);
//        if(parsedAddress == null) {
//          throw new RuntimeException("Parsed "+serviceName+" by channel config fail.");
//        }
//      }
//    } else {
//      String defaultChannelName = protocolTypeProperties.getDefaultChannelName();
//      if(defaultChannelName != null) {
//        ProtocolTypeHttpChannelProperties channelProperties = protocolTypeProperties.getChannels().get(defaultChannelName);
//        parsedAddress = parseChannel(channelProperties);
//      }
//    }
//    return parsedAddress;
//  }
//
//  private static String parseByChannelName(String channelName, ProtocolTypeHttpProperties protocolTypeProperties) {
//    ProtocolTypeHttpChannelProperties channelProperties = protocolTypeProperties.getChannels().get(channelName);
//    return parseChannel(channelProperties);
//  }
//
//  private static String parseDefaultChannel(ProtocolTypeHttpProperties protocolTypeProperties) {
//    String defaultChannelName = protocolTypeProperties.getDefaultChannelName();
//    if(defaultChannelName != null) {
//      ProtocolTypeHttpChannelProperties channelProperties = protocolTypeProperties.getChannels().get(defaultChannelName);
//      String parsedAddress = parseChannel(channelProperties);
//      if(parsedAddress == null) {
//        throw new RuntimeException("Parsed defaultChannelName "+defaultChannelName+" fail.");
//      }
//      return parsedAddress;
//    }
//    return null;
//  }
//
//  private static String parseChannel(ProtocolTypeHttpChannelProperties channelProperties) {
//    String parsedAddress = null;
//    if(channelProperties != null) {
//      String address = channelProperties.getAddress();
//      if(address != null) {
//        parsedAddress = address;
//      } else {
//        List<String> addresses = channelProperties.getAddresses();
//        if(!addresses.isEmpty()) {
//          parsedAddress = addresses.get(new Random().nextInt(addresses.size()));
//        }
//      }
//    }
//    return parsedAddress;
//  }
//
//}
