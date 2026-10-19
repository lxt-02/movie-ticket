package com.mv.movieservice.application.dto;

import java.util.List;

/** One page of a search result, independent of Spring Data. */
public record PageResult<T>(List<T> items, int page, int size, long totalElements) {
    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
