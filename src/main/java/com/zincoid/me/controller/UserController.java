package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
import com.zincoid.me.model.dto.PasswordChangeRequest;
import com.zincoid.me.model.dto.UserUpdateRequest;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.UserCardVO;
import com.zincoid.me.model.vo.UserDetailVO;
import com.zincoid.me.model.dto.UserConfigUpdateRequest;
import com.zincoid.me.model.vo.UserConfigVO;
import com.zincoid.me.service.UserConfigService;
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
    private final UserConfigService userConfigService;

    // ──── Private endpoints ───────────────

    @GetMapping
    public ApiResp<UserDetailVO> getCurrentUserDetail() {
        return ApiResp.success(userService.get(AuthCtx.getUserId()));
    }

    @PutMapping
    public ApiResp<UserDetailVO> updateUser(@Valid @RequestBody UserUpdateRequest request) {
        return ApiResp.success(userService.update(AuthCtx.getUserId(), request));
    }

    @PutMapping("/avatar")
    public ApiResp<UserDetailVO> updateUserAvatar(@RequestBody Map<String, String> body) {
        return ApiResp.success(userService.updateAvatar(AuthCtx.getUserId(), body.get("avatar")));
    }

    @DeleteMapping
    public ApiResp<Void> deleteUser() {
        userService.delete(AuthCtx.getUserId());
        return ApiResp.success();
    }

    @PutMapping("/password")
    public ApiResp<Void> changeUserPassword(@Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(AuthCtx.getUserId(), request.getOldPassword(), request.getNewPassword());
        return ApiResp.success();
    }

    @PutMapping("/{userId}/status")
    public ApiResp<Void> updateUserStatus(@PathVariable Long userId,
                                          @RequestParam Status status) {
        AuthCtx.requireAdmin();
        userService.updateStatus(userId, status);
        return ApiResp.success();
    }

    @DeleteMapping("/{userId}")
    public ApiResp<Void> deleteUser(@PathVariable Long userId) {
        AuthCtx.requireAdmin();
        userService.delete(userId);
        return ApiResp.success();
    }

    @PutMapping("/password/force")
    public ApiResp<Void> changeUserPassword(@RequestBody Map<String, String> body) {
        AuthCtx.requireAdmin();
        userService.changePasswordByForce(body.get("username"), body.get("password"));
        return ApiResp.success();
    }

    @GetMapping("/config")
    public ApiResp<UserConfigVO> getUserConfig() {
        return ApiResp.success(userConfigService.get(AuthCtx.getUserId()));
    }

    @PutMapping("/config")
    public ApiResp<UserConfigVO> updateUserConfig(@Valid @RequestBody UserConfigUpdateRequest request) {
        return ApiResp.success(userConfigService.update(AuthCtx.getUserId(), request));
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public")
    public ApiResp<PageVO<UserCardVO>> listUsers(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) Role role,
                                                 @RequestParam(required = false) String keyword) {
        boolean isActive = !(AuthCtx.isAuthed() && AuthCtx.getRole() == Role.ADMIN);
        return ApiResp.success(userService.list(page, size, role, isActive, keyword));
    }

    @GetMapping("/public/{userId}")
    public ApiResp<UserDetailVO> userDetail(@PathVariable Long userId) {
        return ApiResp.success(userService.get(userId));
    }

    @GetMapping("/public/username/{username}")
    public ApiResp<UserDetailVO> userDetail(@PathVariable String username) {
        return ApiResp.success(userService.get(username));
    }
}
