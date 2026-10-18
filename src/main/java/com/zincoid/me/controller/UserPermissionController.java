package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.enums.Permission;
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

    @PostMapping("/{userId}/{permission}")
    public ApiResponse<Void> grantPermission(@PathVariable Long userId, @PathVariable Permission permission) {
        AuthCtx.requireAdmin();
        userPermissionService.grant(userId, permission, AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> revokePermission(@PathVariable Long id) {
        AuthCtx.requireAdmin();
        userPermissionService.revoke(id, AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @GetMapping("/my")
    public ApiResponse<List<Permission>> myPermissions() {
        return ApiResponse.success(userPermissionService.get(AuthCtx.getUserId()));
    }

    @GetMapping
    public ApiResponse<PageVO<UserPermissionVO>> listPermissions(@RequestParam(required = false) Long userId,
                                                                @RequestParam(required = false) Permission permission,
                                                                @RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        AuthCtx.requireAdmin();
        return ApiResponse.success(userPermissionService.list(userId, permission, page, size));
    }
}
