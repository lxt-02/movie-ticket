package com.mv.apigateway.loadbalancer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Per-service load-balancer child context configuration.
 * <p>
 * Intentionally NOT annotated with {@code @Configuration}: it is registered through
 * {@link LoadBalancerConfig} and must not be picked up by component scanning.
 * <p>
 * {@code app.load-balancer.strategy}: {@code round-robin} (default) or {@code random}.
 */
public class LoadBalancerStrategyConfiguration {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        ObjectProvider<ServiceInstanceListSupplier> supplier =
                loadBalancerClientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class);

        String strategy = environment.getProperty("app.load-balancer.strategy", "round-robin");
        if ("random".equalsIgnoreCase(strategy.trim())) {
            return new RandomLoadBalancer(supplier, serviceId);
        }
        return new RoundRobinLoadBalancer(supplier, serviceId);
    }
}
