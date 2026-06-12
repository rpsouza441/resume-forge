package com.resumeforge.common.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;
import java.util.List;

@Data
@Builder
public class PaginatedResponse<T> {
    private List<T> data;
    private int page;
    private int size;
    private long total;
    private int totalPages;

    /**
     * Factory method to create a PaginatedResponse from a Spring Data Page.
     *
     * @param page the Spring Data Page
     * @param <T>  the type of the data elements
     * @return a new PaginatedResponse populated from the Page
     */
    public static <T> PaginatedResponse<T> of(Page<T> page) {
        return PaginatedResponse.<T>builder()
                .data(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}