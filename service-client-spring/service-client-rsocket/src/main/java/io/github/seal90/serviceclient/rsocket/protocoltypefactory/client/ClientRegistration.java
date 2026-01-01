package io.github.seal90.serviceclient.rsocket.protocoltypefactory.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientRegistration {
    private String serviceId;
    private String instanceId;
    private Map<String, String> tags;
}