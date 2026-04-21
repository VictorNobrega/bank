package com.renner.bank.mapper;

import com.renner.bank.dto.pagination.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PageMapper {

    public <T> PaginatedResponse<T> toPaginatedResponse(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumberOfElements(),
                page.getNumber(),
                page.getSize()
        );
    }

    public Pageable buildPagination(Integer page, Integer size) {
        return PageRequest.of(page, size);
    }
}
