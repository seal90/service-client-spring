package io.github.seal90.serviceclient.core.loadbalancer.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import io.github.seal90.serviceclient.core.loadbalancer.DiscoveryClient;
import io.github.seal90.serviceclient.core.loadbalancer.ServiceInstance;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * A {@link DiscoveryClient} that is composed of other discovery clients and delegates
 * calls to each of them in order.
 *
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 * @author Sean Ruffatti
 */
public class CompositeDiscoveryClient implements DiscoveryClient {

	private final List<DiscoveryClient> discoveryClients;

	public CompositeDiscoveryClient(List<DiscoveryClient> discoveryClients) {
		AnnotationAwareOrderComparator.sort(discoveryClients);
		this.discoveryClients = discoveryClients;
	}

	@Override
	public String description() {
		return "Composite Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		if (this.discoveryClients != null) {
			for (DiscoveryClient discoveryClient : this.discoveryClients) {
				List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
				if (instances != null && !instances.isEmpty()) {
					return instances;
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	public List<String> getServices() {
		LinkedHashSet<String> services = new LinkedHashSet<>();
		if (this.discoveryClients != null) {
			for (DiscoveryClient discoveryClient : this.discoveryClients) {
				List<String> serviceForClient = discoveryClient.getServices();
				if (serviceForClient != null) {
					services.addAll(serviceForClient);
				}
			}
		}
		return new ArrayList<>(services);
	}

	@Override
	public void probe() {
		if (this.discoveryClients != null) {
			for (DiscoveryClient discoveryClient : this.discoveryClients) {
				discoveryClient.probe();
			}
		}
	}

	public List<DiscoveryClient> getDiscoveryClients() {
		return this.discoveryClients;
	}

}