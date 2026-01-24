package com.prism.prism_auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prism.prism_auth.dto.PagedResponse;
import com.prism.prism_auth.model.User;
import com.prism.prism_auth.service.AdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/get-all-users")
    public PagedResponse<User> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info(
                "[ADMIN-CONTROLLER] Admin requested user list: search='{}', sortBy='{}', sortDir='{}', page={}, size={}",
                search, sortBy, sortDir, page, size);
        return adminService.findAllUsers(search, sortBy, sortDir, page, size);
    }

    
}
