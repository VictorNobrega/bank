package com.renner.bank.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageMapperTest {

    private final PageMapper pageMapper = new PageMapper();

    @Test
    void shouldBuildPaginationWithRequestedPageAndSize() {
        var pageable = pageMapper.buildPagination(2, 15);

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(15);
    }

    @Test
    void shouldConvertPageToPaginatedResponse() {
        var pageable = PageRequest.of(1, 2);
        var page = new PageImpl<>(List.of("a", "b"), pageable, 5);

        var response = pageMapper.toPaginatedResponse(page);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.numberOfElements()).isEqualTo(2);
        assertThat(response.pageNumber()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
    }
}
