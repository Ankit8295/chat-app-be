package com.thechat.common.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasMore
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean hasMore = page + 1 < totalPages;
        return new PageResponse<>(content, page, size, totalElements, totalPages, hasMore);
    }
}
