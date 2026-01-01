package io.github.seal90.serviceclient.util;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

public final class ApplicationContextBeanLookupUtils {

	private ApplicationContextBeanLookupUtils() {
	}

	public static <B, A extends Annotation> LinkedHashMap<B, A> getOrderedBeansWithAnnotation(
			ApplicationContext applicationContext, Class<B> beanType, Class<A> annotationType) {
		Assert.notNull(applicationContext, () -> "applicationContext must not be null");
		var annotatedBeanNamesToBeans = applicationContext.getBeansWithAnnotation(annotationType);
		var annotatedBeansToBeanNames = new LinkedHashMap<Object, String>();
		annotatedBeanNamesToBeans.forEach((name, bean) -> annotatedBeansToBeanNames.put(bean, name));
		var orderedBeans = new LinkedHashMap<B, A>();
		applicationContext.getBeanProvider(beanType).orderedStream().forEachOrdered((b) -> {
			var beanName = annotatedBeansToBeanNames.get(b);
			A beanAnnotation = null;
			if (beanName != null) {
				beanAnnotation = applicationContext.findAnnotationOnBean(beanName, annotationType);
			}
			orderedBeans.put(b, beanAnnotation);
		});
		return orderedBeans;
	}

	public static <B, A extends Annotation> List<B> getBeansWithAnnotation(ApplicationContext applicationContext,
			Class<B> beanType, Class<A> annotationType) {
		Assert.notNull(applicationContext, () -> "applicationContext must not be null");
		var nameToBeanMap = applicationContext.getBeansWithAnnotation(annotationType);
		var beanToNameMap = new LinkedHashMap<Object, String>();
		nameToBeanMap.forEach((name, bean) -> beanToNameMap.put(bean, name));
		return applicationContext.getBeanProvider(beanType).orderedStream().filter(beanToNameMap::containsKey).toList();
	}

	public static void sortBeansIncludingOrderAnnotation(ApplicationContext applicationContext, Class<?> beanType,
			List<?> beans) {
		var beanToNameMap = new LinkedHashMap<Object, String>();
		applicationContext.getBeansOfType(beanType).forEach((name, bean) -> beanToNameMap.put(bean, name));
		beans.sort(OrderComparator.INSTANCE.withSourceProvider(bean -> {
			Integer priority = AnnotationAwareOrderComparator.INSTANCE.getPriority(bean);
			if (priority != null) {
				return (Ordered) () -> priority;
			}
			// Consult the bean factory method for annotations
			String beanName = beanToNameMap.get(bean);
			if (beanName != null) {
				Order order = applicationContext.findAnnotationOnBean(beanName, Order.class);
				if (order != null) {
					return (Ordered) order::value;
				}
			}
			return null;
		}));
	}

}