package vn.edu.hust.soict.soe.assetmanagement.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.user.dto.CreateUserRequest;
import vn.edu.hust.soict.soe.assetmanagement.user.dto.UserDto;
import vn.edu.hust.soict.soe.assetmanagement.user.service.UserService;

import java.util.List;
import java.util.UUID;

/**
 * User management endpoints.
 * All routes except /me are restricted to SYSTEM_ADMIN.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "SYSTEM_ADMIN only — manage users and roles")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // GET /api/users/me — any authenticated user
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(Authentication authentication) {
        
        // Extract username from Authentication object, then fetch UserDto using UserService
        String username = authentication.getName();
        
        // UserService returns a UserDto which is safe to return from API (no password hash)
        UserDto currentUserDto = userService.getUserByUsername(username); 
        
        return ResponseEntity.ok(
                ApiResponse.success("Tải thông tin người dùng thành công.", currentUserDto));
    }

    // GET /api/users — SYSTEM_ADMIN only
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(
                ApiResponse.success(userService.getAllUsers()));
    }

    // GET /api/users/{id} — SYSTEM_ADMIN only
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.getUserById(id)));
    }

    // POST /api/users — SYSTEM_ADMIN only
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new user")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo người dùng thành công.", created));
    }

    // PATCH /api/users/{id}/deactivate — SYSTEM_ADMIN only
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Deactivate a user account")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("Tài khoản đã bị vô hiệu hóa."));
    }
}