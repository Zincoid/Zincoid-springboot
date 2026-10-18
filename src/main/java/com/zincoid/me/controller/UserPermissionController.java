package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.enums.PermissionType;
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

    @PostMapping("/{userId}/{type}")
    public ApiResponse<Void> grantPermission(@PathVariable Long userId, @PathVariable PermissionType type) {
        AuthCtx.requireAdmin();
        userPermissionService.grant(userId, type, AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> revokePermission(@PathVariable Long id) {
        AuthCtx.requireAdmin();
        userPermissionService.revoke(id, AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @GetMapping("/my")
    public ApiResponse<List<PermissionType>> myPermissions() {
        return ApiResponse.success(userPermissionService.get(AuthCtx.getUserId()));
    }

    @GetMapping
    public ApiResponse<PageVO<UserPermissionVO>> listPermissions(@RequestParam(required = false) Long userId,
                                                                @RequestParam(required = false) PermissionType type,
                                                                @RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        AuthCtx.requireAdmin();
        return ApiResponse.success(userPermissionService.list(userId, type, page, size));
    }
}
