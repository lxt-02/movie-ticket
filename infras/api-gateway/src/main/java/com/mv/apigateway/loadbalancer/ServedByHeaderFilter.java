package com.mv.apigateway.loadbalancer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adds {@code X-Served-By} with the downstream instance the load balancer chose
 * (Eureka instance ID, or host:port), so a demo can show requests spread across instances.
 * Disable with {@code app.load-balancer.served-by-header=false}.
 */
@Component
public class ServedByHeaderFilter implements GlobalFilter, Ordered {

    static final String SERVED_BY_HEADER = "X-Served-By";

    private final boolean enabled;

    public ServedByHeaderFilter(@Value("${app.load-balancer.served-by-header:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (enabled) {
            Response<ServiceInstance> lbResponse =
                    exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR);
            if (lbResponse != null && lbResponse.hasServer()) {
                ServiceInstance instance = lbResponse.getServer();
                String servedBy = instance.getInstanceId() != null
                        ? instance.getInstanceId()
                        : instance.getHost() + ":" + instance.getPort();
                exchange.getResponse().getHeaders().set(SERVED_BY_HEADER, servedBy);
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Runs right after ReactiveLoadBalancerClientFilter has chosen the instance.
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER + 1;
    }
}
