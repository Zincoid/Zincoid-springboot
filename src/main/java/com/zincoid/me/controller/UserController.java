package com.zincoid.me.controller;

import com.zincoid.me.model.dto.PasswordChangeRequest;
import com.zincoid.me.model.dto.UserUpdateRequest;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.UserCardVO;
import com.zincoid.me.model.vo.UserDetailVO;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ──── Private endpoints ────────────────

    @GetMapping
    public ApiResponse<UserDetailVO> getCurrentUserDetail() {
        return ApiResponse.success(userService.get(AuthCtx.getUserId()));
    }

    @PutMapping
    public ApiResponse<UserDetailVO> updateUser(@Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(AuthCtx.getUserId(), request));
    }

    @PutMapping("/avatar")
    public ApiResponse<UserDetailVO> updateAvatar(@RequestBody Map<String, String> body) {
        String avatar = body.get("avatar");
        if (avatar == null || avatar.isBlank())
            return ApiResponse.badRequest("Avatar is required");
        return ApiResponse.success(userService.updateAvatar(AuthCtx.getUserId(), avatar));
    }

    @DeleteMapping
    public ApiResponse<Void> deleteUser() {
        userService.delete(AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(AuthCtx.getUserId(), request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success();
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long userId,
                                              @RequestParam Status status) {
        AuthCtx.requireAdmin();
        userService.updateStatus(userId, status);
        return ApiResponse.success();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        AuthCtx.requireAdmin();
        userService.delete(userId);
        return ApiResponse.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public")
    public ApiResponse<PageVO<UserCardVO>> listUsers(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size,
                                                     @RequestParam(required = false) Role role) {
        boolean isActive = !(AuthCtx.isAuthed() && AuthCtx.getRole() == Role.ADMIN);
        return ApiResponse.success(userService.list(page, size, role, isActive));
    }

    @GetMapping("/public/{userId}")
    public ApiResponse<UserDetailVO> userDetail(@PathVariable Long userId) {
        return ApiResponse.success(userService.get(userId));
    }
}
