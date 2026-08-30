/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.invocation.reactive;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageRequest;
import io.github.seal90.serviceclient.carries.by.rsocket.context.MessageResponse;
import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.MessageRSocket;
import io.github.seal90.serviceclient.carries.by.rsocket.context.rsocket.MessageRSocketResponderInterceptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.*;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.util.ObjectUtils;

/**
 * Extension of {@link HandlerMethod} that invokes the underlying method with
 * argument values resolved from the current HTTP request through a list of
 * {@link HandlerMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private static final Mono<Object[]> EMPTY_ARGS = Mono.just(new Object[0]);

	private static final Object NO_ARG_VALUE = new Object();


	private final HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

	/**
	 * Create an instance from a {@code HandlerMethod}.
	 */
	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	/**
	 * Create an instance from a bean instance and a method.
	 */
	public InvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}


	/**
	 * Configure the argument resolvers to use for resolving method
	 * argument values against a {@code ServerWebExchange}.
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.resolvers.addResolvers(resolvers);
	}

	/**
	 * Return the configured argument resolvers.
	 */
	public List<HandlerMethodArgumentResolver> getResolvers() {
		return this.resolvers.getResolvers();
	}

	/**
	 * Set the ParameterNameDiscoverer for resolving parameter names when needed
	 * (for example, default request attribute name).
	 * <p>Default is a {@link DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer nameDiscoverer) {
		this.parameterNameDiscoverer = nameDiscoverer;
	}

	/**
	 * Return the configured parameter name discoverer.
	 */
	public ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}

	/**
	 * Configure a reactive adapter registry. This is needed for async return values.
	 * <p>By default this is a {@link ReactiveAdapterRegistry} with default settings.
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		this.reactiveAdapterRegistry = registry;
	}


	/**
	 * Invoke the method for the given exchange.
	 * @param message the current message
	 * @param providedArgs optional list of argument values to match by type
	 * @return a Mono with the result from the invocation
	 */
	@SuppressWarnings("KotlinInternalInJava")
	public Mono<Object> invoke(Message<?> message, Object... providedArgs) {
		return getMethodArgumentValues(message, providedArgs).flatMap(args -> {
			Object value = null;
			boolean isSuspendingFunction = false;
			boolean executed = false;
			try {
				Method method = getBridgedMethod();
				if (KotlinDetector.isSuspendingFunction(method)) {
					isSuspendingFunction = true;
					value = CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);
				}
				else {
					Class<?> returnType = method.getReturnType();
					Class<?>[] parameterTypes = method.getParameterTypes();
					if(Mono.class.isAssignableFrom(returnType) && parameterTypes.length == 1
							&& Mono.class.isAssignableFrom(parameterTypes[0])) {
						Class<?> returnGenericType = ResolvableType.forMethodReturnType(method).as(Mono.class).getGeneric(0).resolve();
						Class<?> parameterGenericType = ResolvableType.forMethodParameter(method, 0).getGeneric(0).resolve();
						if(MessageResponse.class.isAssignableFrom(returnGenericType) && MessageRequest.class.isAssignableFrom(parameterGenericType)) {
							BeanFactory beanFactory = super.getBeanFactory();
							if (beanFactory != null) {
								Map<String, MessageRSocketResponderInterceptor> rSocketResponderInterceptorMap =
										BeanFactoryUtils.beansOfTypeIncludingAncestors((ListableBeanFactory) beanFactory, MessageRSocketResponderInterceptor.class);
								List<MessageRSocketResponderInterceptor> interceptors = new ArrayList<>(rSocketResponderInterceptorMap.values());
								AnnotationAwareOrderComparator.sort(interceptors);
								MessageRSocket execMessageRSocket = new MessageRSocket() {

									@Override
									public Mono<Void> fireAndForget(Mono<MessageRequest> request) {
										return null;
									}

									@Override
									public Mono<MessageResponse> requestResponse(Mono<MessageRequest> request) {
										try {
											return (Mono<MessageResponse>) method.invoke(getBean(), request);
										} catch (Exception e) {
											throw new RuntimeException(e);
										}
									}

									@Override
									public Flux<MessageResponse> requestStream(Mono<MessageRequest> request) {
										return null;
									}

									@Override
									public Flux<MessageResponse> requestChannel(Flux<MessageRequest> requests) {
										try {
											return (Flux<MessageResponse>) method.invoke(getBean(), requests);
										} catch (Exception e) {
											throw new RuntimeException(e);
										}
									}

									@Override
									public Mono<Void> metadataPush(Mono<MessageRequest> request) {
										return null;
									}
								};
								MessageRSocket messageRSocket = interceptors.stream()
										.reduce(
												execMessageRSocket,
												(current, interceptor) -> interceptor.apply(current),
												(a, b) -> a);

								value = messageRSocket.requestResponse((Mono<MessageRequest>) args[0]);
								executed = true;

							}
						}

					}
					if(Flux.class.isAssignableFrom(returnType) && parameterTypes.length == 1
							&& Flux.class.isAssignableFrom(parameterTypes[0])) {
						Class<?> returnGenericType = ResolvableType.forMethodReturnType(method).as(Flux.class).getGeneric(0).resolve();
						Class<?> parameterGenericType = ResolvableType.forMethodParameter(method, 0).getGeneric(0).resolve();
						if(MessageResponse.class.isAssignableFrom(returnGenericType) && MessageRequest.class.isAssignableFrom(parameterGenericType)) {
							BeanFactory beanFactory = super.getBeanFactory();
							Map<String, MessageRSocketResponderInterceptor> rSocketResponderInterceptorMap =
									BeanFactoryUtils.beansOfTypeIncludingAncestors((ListableBeanFactory)beanFactory, MessageRSocketResponderInterceptor.class);
							List<MessageRSocketResponderInterceptor> interceptors = new ArrayList<>(rSocketResponderInterceptorMap.values());
							AnnotationAwareOrderComparator.sort(interceptors);
							MessageRSocket execMessageRSocket = new MessageRSocket() {

								@Override
								public Mono<Void> fireAndForget(Mono<MessageRequest> request) {
									return null;
								}

								@Override
								public Mono<MessageResponse> requestResponse(Mono<MessageRequest> request) {
                                    try {
                                        return (Mono<MessageResponse>)method.invoke(getBean(), request);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
								}

								@Override
								public Flux<MessageResponse> requestStream(Mono<MessageRequest> request) {
									return null;
								}

								@Override
								public Flux<MessageResponse> requestChannel(Flux<MessageRequest> requests) {
                                    try {
                                        return (Flux<MessageResponse>)method.invoke(getBean(), requests);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }

								@Override
								public Mono<Void> metadataPush(Mono<MessageRequest> request) {
									return null;
								}
							};

							MessageRSocket messageRSocket = interceptors.stream()
									.reduce(
											execMessageRSocket,
											(current, interceptor) -> interceptor.apply(current),
											(a, b) -> a);

							value = messageRSocket.requestChannel((Flux<MessageRequest>)args[0]);
							executed = true;
						}
					}
					if(!executed) {
						value = method.invoke(getBean(), args);
					}
				}
			}
			catch (IllegalArgumentException ex) {
				assertTargetBean(getBridgedMethod(), getBean(), args);
				String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
				return Mono.error(new IllegalStateException(formatInvokeError(text, args), ex));
			}
			catch (InvocationTargetException ex) {
				return Mono.error(ex.getTargetException());
			}
			catch (Throwable ex) {
				// Unlikely to ever get here, but it must be handled...
				return Mono.error(new IllegalStateException(formatInvokeError("Invocation failure", args), ex));
			}

			MethodParameter returnType = getReturnType();
			Class<?> reactiveType = (isSuspendingFunction ? value.getClass() : returnType.getParameterType());
			ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(reactiveType);
			return (adapter != null && isAsyncVoidReturnType(returnType, adapter) ?
					Mono.from(adapter.toPublisher(value)) : Mono.justOrEmpty(value));
		});
	}

	private Mono<Object[]> getMethodArgumentValues(Message<?> message, Object... providedArgs) {
		MethodParameter[] parameters = getMethodParameters();
		if (ObjectUtils.isEmpty(getMethodParameters())) {
			return EMPTY_ARGS;
		}

		List<Mono<Object>> argMonos = new ArrayList<>(parameters.length);
		for (MethodParameter parameter : parameters) {
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
			Object providedArg = findProvidedArgument(parameter, providedArgs);
			if (providedArg != null) {
				argMonos.add(Mono.just(providedArg));
				continue;
			}
			if (!this.resolvers.supportsParameter(parameter)) {
				return Mono.error(new MethodArgumentResolutionException(
						message, parameter, formatArgumentError(parameter, "No suitable resolver")));
			}
			try {
				argMonos.add(this.resolvers.resolveArgument(parameter, message)
						.defaultIfEmpty(NO_ARG_VALUE)
						.doOnError(ex -> logArgumentErrorIfNecessary(parameter, ex)));
			}
			catch (Exception ex) {
				logArgumentErrorIfNecessary(parameter, ex);
				argMonos.add(Mono.error(ex));
			}
		}
		return Mono.zip(argMonos, values ->
				Stream.of(values).map(value -> value != NO_ARG_VALUE ? value : null).toArray());
	}

	private void logArgumentErrorIfNecessary(MethodParameter parameter, Throwable ex) {
		// Leave stack trace for later, if error is not handled...
		String exMsg = ex.getMessage();
		if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
			if (logger.isDebugEnabled()) {
				logger.debug(formatArgumentError(parameter, exMsg));
			}
		}
	}

	private boolean isAsyncVoidReturnType(MethodParameter returnType, ReactiveAdapter reactiveAdapter) {
		if (reactiveAdapter.supportsEmpty()) {
			if (reactiveAdapter.isNoValue()) {
				return true;
			}
		}
		Type parameterType = returnType.getGenericParameterType();
		if (parameterType instanceof ParameterizedType type) {
			if (type.getActualTypeArguments().length == 1) {
				return Void.class.equals(type.getActualTypeArguments()[0]);
			}
		}
		Method method = returnType.getMethod();
		return method != null && KotlinDetector.isSuspendingFunction(method) &&
				(returnType.getParameterType() == void.class);
	}

}
