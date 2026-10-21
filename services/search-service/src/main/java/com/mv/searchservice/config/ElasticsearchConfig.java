package com.mv.searchservice.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * HTTP client for the Elasticsearch REST API. Elasticsearch is called through plain JSON over HTTP
 * (index, delete, _search), which keeps every query visible in the code and needs no extra client library.
 */
@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClient elasticsearchRestClient(ObjectProvider<RestClient.Builder> builders, SearchProperties properties) {
        SearchProperties.Elasticsearch es = properties.elasticsearch();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(es.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(es.readTimeoutMs()));
        // The Spring-managed builder (when present) adds tracing / metrics to every Elasticsearch call.
        RestClient.Builder builder = builders.getIfAvailable(RestClient::builder).clone();
        builder.baseUrl(es.url())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (es.hasCredentials()) {
            String token = Base64.getEncoder().encodeToString(
                    (es.username() + ":" + (es.password() == null ? "" : es.password())).getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + token);
        }
        return builder.build();
    }
}
