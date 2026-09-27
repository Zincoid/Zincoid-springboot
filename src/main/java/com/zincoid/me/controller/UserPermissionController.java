package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.enums.Perm;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.UserPermissionVO;
import com.zincoid.me.service.UserPermissionService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class UserPermissionController {

    private final UserPermissionService userPermissionService;

    // ──── Private endpoints ───────────────

    @PostMapping("/{userId}/{perm}")
    public ApiResponse<Void> grantPermission(@PathVariable Long userId, @PathVariable Perm perm) {
        AuthCtx.requireAdmin();
        userPermissionService.grant(userId, perm, AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @DeleteMapping("/{userId}/{perm}")
    public ApiResponse<Void> revokePermission(@PathVariable Long userId, @PathVariable Perm perm) {
        AuthCtx.requireAdmin();
        userPermissionService.revoke(userId, perm, AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @GetMapping("/my")
    public ApiResponse<List<Perm>> myPermissions() {
        return ApiResponse.success(userPermissionService.get(AuthCtx.getUserId()));
    }

    @GetMapping
    public ApiResponse<PageVO<UserPermissionVO>> listPermissions(@RequestParam(required = false) Long userId,
                                                                @RequestParam(required = false) Perm perm,
                                                                @RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        AuthCtx.requireAdmin();
        return ApiResponse.success(userPermissionService.list(userId, perm, page, size));
    }
}
