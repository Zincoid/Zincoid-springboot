package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.RequestMapper;
import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.enums.RequestType;
import com.zincoid.me.model.po.Notification;
import com.zincoid.me.model.po.Request;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RequestVO;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.RequestService;
import com.zincoid.me.service.StorageService;
import com.zincoid.me.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl extends ServiceImpl<RequestMapper, Request> implements RequestService {

    private static final long ADMIN_UNHANDLED = -1L;
    private static final Set<RequestType> ADMIN_ONLY = Set.of(
            RequestType.STORAGE_EXTENSION,
            RequestType.REPORT
    );

    private final UserService userService;
    private final NotificationService notificationService;
    private final StorageService storageService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    @Transactional
    public RequestVO create(Long senderId, Long receiverId, RequestType type, String meta) {
        if (type == null)
            throw new BusinessException(400, "Request type is invalid");
        if (ADMIN_ONLY.contains(type)) {
            receiverId = ADMIN_UNHANDLED;
            JsonNode node = parseMeta(meta);
            switch (type) {
                case STORAGE_EXTENSION -> {
                    if (fieldLong(node, "expansion") == null)
                        throw new BusinessException(400, "Invalid request meta");
                }
                case REPORT -> {
                    if (fieldText(node, "title") == null || fieldText(node, "content") == null)
                        throw new BusinessException(400, "Invalid request meta");
                }
            }
        } else if (senderId.equals(receiverId))
            throw new BusinessException(400, "Sender and receiver cannot be the same");
        else if (userService.getById(receiverId) == null)
            throw new BusinessException(404, "User not found");
        Request request = Request.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(type)
                .meta(meta)
                .access(Access.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        save(request);
        notificationService.notifyReq(
                senderId, receiverId,
                "Pending",
                NotificationType.REQUEST,
                request.getId(),
                ADMIN_ONLY.contains(type)
        );
        log.info("Request created: id={}, sender={}, type={}", request.getId(), senderId, type);
        return toVO(request, userService.getById(senderId), userService.getById(receiverId));
    }

    @Override
    public PageVO<RequestVO> sent(Long userId, int page, int size) {
        Page<Request> result = lambdaQuery()
                .eq(Request::getSenderId, userId)
                .orderByDesc(Request::getCreatedAt)
                .page(Page.of(page, size));
        List<RequestVO> vos = new ArrayList<>();
        for (Request r : result.getRecords())
            vos.add(toVO(r, userService.getById(r.getSenderId()), userService.getById(r.getReceiverId())));
        return PageVO.of(result, vos);
    }

    @Override
    public PageVO<RequestVO> received(Long userId, int page, int size, boolean isAdmin) {
        Page<Request> result = lambdaQuery()
                .and(w -> {
                    w.eq(Request::getReceiverId, userId);
                    if (isAdmin) w.or().in(Request::getType, ADMIN_ONLY);
                })
                .orderByDesc(Request::getCreatedAt)
                .page(Page.of(page, size));
        List<RequestVO> vos = new ArrayList<>();
        for (Request r : result.getRecords())
            vos.add(toVO(r, userService.getById(r.getSenderId()), userService.getById(r.getReceiverId())));
        return PageVO.of(result, vos);
    }

    @Override
    @Transactional
    public RequestVO handle(Long userId, Long requestId, Access access, boolean isAdmin) {
        if (access == null || access == Access.PENDING)
            throw new BusinessException(400, "Invalid handle status");
        Request request = getById(requestId);
        if (request == null)
            throw new BusinessException(404, "Request not found");
        if ((ADMIN_ONLY.contains(request.getType()) && !isAdmin)
                || (!request.getReceiverId().equals(userId) && request.getReceiverId() != ADMIN_UNHANDLED))
            throw new BusinessException(403, "No permission to handle this request");
        boolean updated = lambdaUpdate()
                .eq(Request::getId, requestId)
                .eq(Request::getAccess, Access.PENDING)
                .set(Request::getAccess, access)
                .set(Request::getHandledAt, LocalDateTime.now())
                .set(ADMIN_ONLY.contains(request.getType()), Request::getReceiverId, userId)
                .update();
        if (!updated)
            throw new BusinessException(400, "Request already handled");
        if (access == Access.APPROVED) apply(request);
        if (!request.getSenderId().equals(userId))
            notificationService.notifyReq(
                    userId, request.getSenderId(),
                    access == Access.APPROVED ? "Approved" : "Rejected",
                    NotificationType.REQUEST,
                    requestId,
                    false
            );
        log.info("Request handled: id={}, by={}, access={}", requestId, userId, access);
        request.setAccess(access);
        request.setHandledAt(LocalDateTime.now());
        return toVO(request, userService.getById(request.getSenderId()), userService.getById(request.getReceiverId()));
    }

    @Override
    public void delete(Long userId, Long requestId, boolean isAdmin) {
        Request request = getById(requestId);
        if (request == null)
            throw new BusinessException(404, "Request not found");
        boolean canDelete = request.getSenderId().equals(userId)
                || request.getReceiverId().equals(userId)
                || (isAdmin && ADMIN_ONLY.contains(request.getType()) && request.getReceiverId() == ADMIN_UNHANDLED);
        if (!canDelete)
            throw new BusinessException(403, "No permission to delete this request");
        removeById(requestId);
        notificationService.deleteAll(NotificationType.REQUEST, requestId);
        log.info("Request deleted: id={}, by={}", requestId, userId);
    }

    @Override
    @Transactional
    public int cleanupExpired(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Long> ids = lambdaQuery()
                .select(Request::getId)
                .and(w -> w
                        .and(q -> q.isNotNull(Request::getHandledAt).lt(Request::getHandledAt, cutoff))
                        .or()
                        .and(q -> q.isNull(Request::getHandledAt).lt(Request::getCreatedAt, cutoff)))
                .list().stream().map(Request::getId).toList();
        if (!ids.isEmpty()) {
            notificationService.lambdaUpdate()
                    .eq(Notification::getRelatedType, NotificationType.REQUEST)
                    .in(Notification::getRelatedId, ids)
                    .remove();
            removeBatchByIds(ids);
        }
        log.info("Request expired cleared: count={}", ids.size());
        return ids.size();
    }

    // ──────── Private tool ────────────────────────────────

    private void apply(Request request) {
        if (request.getType() == RequestType.STORAGE_EXTENSION) {
            Long expansion = fieldLong(parseMeta(request.getMeta()), "expansion");
            if (expansion == null || expansion < 0)
                throw new BusinessException(400, "Invalid request meta");
            storageService.expandCapacity(request.getSenderId(), expansion);
            log.info("Storage expansion applied: user={}, expansion={}", request.getSenderId(), expansion);
        }
    }

    private JsonNode parseMeta(String meta) {
        if (meta == null || meta.isBlank()) return null;
        try {
            return MAPPER.readTree(meta);
        } catch (Exception e) {
            log.warn("Failed to parse request meta: {}", meta, e);
            return null;
        }
    }

    private Long fieldLong(JsonNode node, String field) {
        JsonNode value = node != null ? node.get(field) : null;
        return value != null && value.isNumber() ? value.asLong() : null;
    }

    private String fieldText(JsonNode node, String field) {
        JsonNode value = node != null ? node.get(field) : null;
        return value != null && !value.asText().isBlank() ? value.asText() : null;
    }

    private RequestVO toVO(Request request, User sender, User receiver) {
        return RequestVO.builder()
                .id(request.getId())
                .senderId(request.getSenderId())
                .senderName(sender != null ? sender.getNickname() : null)
                .senderAvatar(sender != null ? sender.getAvatar() : null)
                .receiverId(request.getReceiverId())
                .receiverName(receiver != null ? receiver.getNickname() : null)
                .receiverAvatar(receiver != null ? receiver.getAvatar() : null)
                .type(request.getType())
                .meta(request.getMeta())
                .access(request.getAccess())
                .handledAt(request.getHandledAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
