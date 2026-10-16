package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
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
    public ApiResp<Void> grantPermission(@PathVariable Long userId, @PathVariable Perm perm) {
        AuthCtx.requireAdmin();
        userPermissionService.grant(userId, perm, AuthCtx.getUserId());
        return ApiResp.success();
    }

    @DeleteMapping("/{id}")
    public ApiResp<Void> revokePermission(@PathVariable Long id) {
        AuthCtx.requireAdmin();
        userPermissionService.revoke(id, AuthCtx.getUserId());
        return ApiResp.success();
    }

    @GetMapping("/my")
    public ApiResp<List<Perm>> myPermissions() {
        return ApiResp.success(userPermissionService.get(AuthCtx.getUserId()));
    }

    @GetMapping
    public ApiResp<PageVO<UserPermissionVO>> listPermissions(@RequestParam(required = false) Long userId,
                                                             @RequestParam(required = false) Perm perm,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        AuthCtx.requireAdmin();
        return ApiResp.success(userPermissionService.list(userId, perm, page, size));
    }
}
