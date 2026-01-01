package io.github.seal90.serviceclient.core.loadbalancer;

public interface Request<C> {

	// Avoid breaking backward compatibility
	default C getContext() {
		return null;
	}

	// TODO: define contents

}