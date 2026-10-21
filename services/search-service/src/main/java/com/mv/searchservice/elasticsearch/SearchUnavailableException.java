package com.mv.searchservice.elasticsearch;

/** Elasticsearch cannot be reached or answered with a server error. Retryable. */
public class SearchUnavailableException extends RuntimeException {

    public SearchUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
