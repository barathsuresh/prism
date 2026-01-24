package com.prism.prism_auth.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prism.prism_auth.dto.PagedResponse;
import com.prism.prism_auth.model.User;
import com.prism.prism_auth.repository.UserRepository;
import com.prism.prism_auth.utils.PaginationUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Find all users with pagination and optional search.
     * @param search
     * @param sortBy
     * @param sortDir
     * @param page
     * @param size
     * @return
     */
    @Transactional(readOnly = true) // Add readOnly for optimization
    public PagedResponse<User> findAllUsers(String search, String sortBy, String sortDir, int page, int size) {
        Pageable pageable = PaginationUtils.createPageable(page, size, sortBy, sortDir);
        Page<User> userPage;
        if (search != null && !search.isEmpty()) {
            userPage = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search,
                    pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }
        return new PagedResponse<>(userPage.getContent(), userPage.getNumber(), userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(), userPage.isLast());
    }
}
