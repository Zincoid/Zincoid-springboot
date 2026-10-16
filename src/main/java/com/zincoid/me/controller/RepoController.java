package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
import com.zincoid.me.model.dto.RepoCreateRequest;
import com.zincoid.me.model.dto.RepoUpdateRequest;
import com.zincoid.me.model.enums.RepoType;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoCardVO;
import com.zincoid.me.model.vo.RepoDetailVO;
import com.zincoid.me.model.vo.RepoItemVO;
import com.zincoid.me.service.RepoService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoController {

    private final RepoService repoService;

    // ──── Private endpoints ───────────────

    @PostMapping
    public ApiResp<RepoDetailVO> createRepo(@Valid @RequestBody RepoCreateRequest request) {
        return ApiResp.success(repoService.create(AuthCtx.getUserId(), request));
    }

    @PutMapping("/{repoId}")
    public ApiResp<RepoDetailVO> updateRepo(@PathVariable Long repoId,
                                            @Valid @RequestBody RepoUpdateRequest request) {
        return ApiResp.success(repoService.update(AuthCtx.getUserId(), repoId, request));
    }

    @DeleteMapping("/{repoId}")
    public ApiResp<Void> deleteRepo(@PathVariable Long repoId) {
        repoService.delete(AuthCtx.getUserId(), repoId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResp.success();
    }

    @PutMapping("/{repoId}/pin")
    public ApiResp<Void> pinRepo(@PathVariable Long repoId) {
        AuthCtx.requireAdmin();
        repoService.pin(repoId);
        return ApiResp.success();
    }

    @PutMapping("/{repoId}/unpin")
    public ApiResp<Void> unpinRepo(@PathVariable Long repoId) {
        AuthCtx.requireAdmin();
        repoService.unpin(repoId);
        return ApiResp.success();
    }

    @PostMapping("/{repoId}/items")
    public ApiResp<RepoItemVO> addRepoItem(@PathVariable Long repoId,
                                           @RequestParam Long fileId) {
        return ApiResp.success(repoService.addItem(AuthCtx.getUserId(), repoId, fileId));
    }

    @DeleteMapping("/{repoId}/items/{itemId}")
    public ApiResp<Void> deleteRepoItem(@PathVariable Long repoId,
                                        @PathVariable Long itemId) {
        repoService.deleteItem(AuthCtx.getUserId(), repoId, itemId);
        return ApiResp.success();
    }

    @PutMapping("/{repoId}/items/sort")
    public ApiResp<Void> sortItems(@PathVariable Long repoId,
                                   @RequestParam Long from,
                                   @RequestParam Long to) {
        repoService.swapItems(AuthCtx.getUserId(), repoId, from, to);
        return ApiResp.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/random")
    public ApiResp<RepoCardVO> randomRepo() {
        return ApiResp.success(repoService.random());
    }

    @GetMapping("/public/home")
    public ApiResp<List<RepoCardVO>> homeRepos(@RequestParam(defaultValue = "10") int size) {
        return ApiResp.success(repoService.home(size));
    }

    @GetMapping("/public")
    public ApiResp<PageVO<RepoCardVO>> listRepos(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) RepoType type,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false, defaultValue = "false") boolean tagged,
                                                 @RequestParam(required = false, defaultValue = "false") boolean updated,
                                                 @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResp.success(repoService.list(type, keyword, tagged, updated, pinned, page, size));
    }

    @GetMapping("/public/user/{userId}")
    public ApiResp<PageVO<RepoCardVO>> userRepos(@PathVariable Long userId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) RepoType type,
                                                 @RequestParam(required = false, defaultValue = "false") boolean updated,
                                                 @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResp.success(repoService.list(type, userId, updated, pinned, page, size));
    }

    @GetMapping("/public/{repoId}")
    public ApiResp<RepoDetailVO> repoDetail(@PathVariable Long repoId) {
        return ApiResp.success(repoService.get(repoId));
    }

    @GetMapping("/public/{repoId}/items")
    public ApiResp<PageVO<RepoItemVO>> repoItems(@PathVariable Long repoId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(defaultValue = "false") boolean latest) {
        return ApiResp.success(repoService.items(repoId, latest, page, size));
    }
}
