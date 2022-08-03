package org.ligson.k8sterminalforward.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

public class ServiceLocator {
    private static BeanFactory beanFactory;

    public static <T> T getService(Class<T> requiredType) {
        if (beanFactory == null) {
            return null;
        }
        return beanFactory.getBean(requiredType);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getServiceByName(String beanName) {
        if (beanFactory == null) {
            return null;
        }
        return (T) beanFactory.getBean(beanName);
    }

    @Component
    @Lazy(value = false)
    static class Aware implements BeanFactoryAware {

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            ServiceLocator.beanFactory = beanFactory;
        }
    }
}
