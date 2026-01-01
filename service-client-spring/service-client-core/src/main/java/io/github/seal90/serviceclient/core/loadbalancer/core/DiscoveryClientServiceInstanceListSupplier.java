package io.github.seal90.serviceclient.core.loadbalancer.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.github.seal90.serviceclient.core.loadbalancer.DiscoveryClient;
import io.github.seal90.serviceclient.core.loadbalancer.ServiceInstance;
import io.github.seal90.serviceclient.core.loadbalancer.ServiceInstanceListSupplier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A discovery-client-based {@link ServiceInstanceListSupplier} implementation.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 * @author Rod Catter
 * @since 2.2.0
 */
public class DiscoveryClientServiceInstanceListSupplier implements ServiceInstanceListSupplier {

	/**
	 * Property that establishes the timeout for calls to service discovery.
	 */
	public static final String SERVICE_DISCOVERY_TIMEOUT = "spring.cloud.loadbalancer.service-discovery.timeout";

	private static final Log LOG = LogFactory.getLog(DiscoveryClientServiceInstanceListSupplier.class);

	private Duration timeout = Duration.ofSeconds(30);

	private final String serviceId;

	private final Flux<List<ServiceInstance>> serviceInstances;

	public DiscoveryClientServiceInstanceListSupplier(DiscoveryClient delegate, String serviceId) {
//		this.serviceId = environment.getProperty(PROPERTY_NAME);
//		resolveTimeout(environment);
		this.serviceId = serviceId;
		this.serviceInstances = Flux.defer(() -> Mono.fromCallable(() -> delegate.getInstances(serviceId)))
			.timeout(timeout, Flux.defer(() -> {
				logTimeout();
				return Flux.just(new ArrayList<>());
			}), Schedulers.boundedElastic())
			.onErrorResume(error -> {
				logException(error);
				return Flux.just(new ArrayList<>());
			});
	}

//	public DiscoveryClientServiceInstanceListSupplier(ReactiveDiscoveryClient delegate, Environment environment) {
//		this.serviceId = environment.getProperty(PROPERTY_NAME);
//		resolveTimeout(environment);
//		this.serviceInstances = Flux
//			.defer(() -> delegate.getInstances(serviceId).collectList().flux().timeout(timeout, Flux.defer(() -> {
//				logTimeout();
//				return Flux.just(new ArrayList<>());
//			})).onErrorResume(error -> {
//				logException(error);
//				return Flux.just(new ArrayList<>());
//			}));
//	}

	@Override
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return serviceInstances;
	}

//	private void resolveTimeout(Environment environment) {
//		String providedTimeout = environment.getProperty(SERVICE_DISCOVERY_TIMEOUT);
//		if (providedTimeout != null) {
//			timeout = DurationStyle.detectAndParse(providedTimeout);
//		}
//	}

	private void logTimeout() {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("Timeout occurred while retrieving instances for service %s."
					+ "The instances could not be retrieved during %s", serviceId, timeout));
		}
	}

	private void logException(Throwable error) {
		if (LOG.isErrorEnabled()) {
			LOG.error(String.format("Exception occurred while retrieving instances for service %s", serviceId), error);
		}
	}

}