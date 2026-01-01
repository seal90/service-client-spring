package io.github.seal90.serviceclient.core.loadbalancer;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Supplier;

public interface ServiceInstanceListSupplier extends Supplier<Flux<List<ServiceInstance>>> {

	String getServiceId();

	default Flux<List<ServiceInstance>> get(Request request) {
		return get();
	}

}