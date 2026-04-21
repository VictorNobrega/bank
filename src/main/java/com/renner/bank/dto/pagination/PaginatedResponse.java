package com.renner.bank.dto.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response")
public record PaginatedResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int numberOfElements,
        int pageNumber,
        int size
) {}
