package io.github.seal90.serviceclient.core;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ServiceClient configuration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ServiceClientProperties.class})
public class ServiceClientConfiguration {

}
