package com.prism.prism_auth.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
@Component
public class PaginationUtils {
    /**
     * Create pageable with sorting
     */
    public static Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null && !sortBy.isBlank() ? sortBy : "createdAt");
        return PageRequest.of(page, size, sort);
    }
}
