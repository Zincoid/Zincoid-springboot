package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
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
    public ApiResponse<RepoDetailVO> createRepo(@Valid @RequestBody RepoCreateRequest request) {
        return ApiResponse.success(repoService.create(AuthCtx.getUserId(), request));
    }

    @PutMapping("/{repoId}")
    public ApiResponse<RepoDetailVO> updateRepo(@PathVariable Long repoId,
                                                @Valid @RequestBody RepoUpdateRequest request) {
        return ApiResponse.success(repoService.update(AuthCtx.getUserId(), repoId, request));
    }

    @DeleteMapping("/{repoId}")
    public ApiResponse<Void> deleteRepo(@PathVariable Long repoId) {
        repoService.delete(AuthCtx.getUserId(), repoId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResponse.success();
    }

    @PutMapping("/{repoId}/pin")
    public ApiResponse<Void> pinRepo(@PathVariable Long repoId) {
        AuthCtx.requireAdmin();
        repoService.pin(repoId);
        return ApiResponse.success();
    }

    @PutMapping("/{repoId}/unpin")
    public ApiResponse<Void> unpinRepo(@PathVariable Long repoId) {
        AuthCtx.requireAdmin();
        repoService.unpin(repoId);
        return ApiResponse.success();
    }

    @PostMapping("/{repoId}/items")
    public ApiResponse<RepoItemVO> addRepoItem(@PathVariable Long repoId,
                                               @RequestParam Long fileId) {
        return ApiResponse.success(repoService.addItem(AuthCtx.getUserId(), repoId, fileId));
    }

    @DeleteMapping("/{repoId}/items/{itemId}")
    public ApiResponse<Void> deleteRepoItem(@PathVariable Long repoId,
                                            @PathVariable Long itemId) {
        repoService.deleteItem(AuthCtx.getUserId(), repoId, itemId);
        return ApiResponse.success();
    }

    @PutMapping("/{repoId}/items/sort")
    public ApiResponse<Void> sortItems(@PathVariable Long repoId,
                                       @RequestParam Long from,
                                       @RequestParam Long to) {
        repoService.swapItems(AuthCtx.getUserId(), repoId, from, to);
        return ApiResponse.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/random")
    public ApiResponse<RepoCardVO> randomRepo() {
        return ApiResponse.success(repoService.random());
    }

    @GetMapping("/public/home")
    public ApiResponse<List<RepoCardVO>> homeRepos(@RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(repoService.home(size));
    }

    @GetMapping("/public")
    public ApiResponse<PageVO<RepoCardVO>> listRepos(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) RepoType type,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false, defaultValue = "false") boolean tagged,
                                                     @RequestParam(required = false, defaultValue = "false") boolean updated,
                                                     @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResponse.success(repoService.list(type, keyword, tagged, updated, pinned, page, size));
    }

    @GetMapping("/public/user/{userId}")
    public ApiResponse<PageVO<RepoCardVO>> userRepos(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) RepoType type,
                                                     @RequestParam(required = false, defaultValue = "false") boolean updated,
                                                     @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResponse.success(repoService.list(type, userId, updated, pinned, page, size));
    }

    @GetMapping("/public/{repoId}")
    public ApiResponse<RepoDetailVO> repoDetail(@PathVariable Long repoId) {
        return ApiResponse.success(repoService.get(repoId));
    }

    @GetMapping("/public/{repoId}/items")
    public ApiResponse<PageVO<RepoItemVO>> repoItems(@PathVariable Long repoId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(repoService.items(repoId, page, size));
    }
}
