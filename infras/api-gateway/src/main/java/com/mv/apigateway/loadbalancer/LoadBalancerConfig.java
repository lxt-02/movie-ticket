package com.mv.apigateway.loadbalancer;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;

/**
 * Client-side load balancing for {@code lb://service-name} routes.
 * Instances come from Eureka; the algorithm is chosen by {@code app.load-balancer.strategy}.
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = LoadBalancerStrategyConfiguration.class)
public class LoadBalancerConfig {
}
