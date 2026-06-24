package com.zincoid.me.controller;

import com.zincoid.me.model.dto.ChangePasswordRequest;
import com.zincoid.me.model.dto.UpdateUserRequest;
import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.UserCardVO;
import com.zincoid.me.model.vo.UserDetailVO;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<UserDetailVO> updateUser(@Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.update(AuthCtx.getUserId(), request));
    }

    @DeleteMapping
    public ApiResponse<Void> deleteUser() {
        userService.delete(AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(AuthCtx.getUserId(), request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public")
    public ApiResponse<PageVO<UserCardVO>> listUsers(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userService.list(page, size));
    }

    @GetMapping("/public/{userId}")
    public ApiResponse<UserDetailVO> userDetail(@PathVariable Long userId) {
        return ApiResponse.success(userService.get(userId));
    }
}
