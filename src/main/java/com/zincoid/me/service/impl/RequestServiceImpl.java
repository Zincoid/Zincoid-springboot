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
import com.zincoid.me.model.po.Request;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RequestVO;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.RequestService;
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
            RequestType.STORAGE_EXTENSION
    );

    private final UserService userService;
    private final NotificationService notificationService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    @Transactional
    public RequestVO create(Long senderId, Long receiverId, RequestType type, String content) {
        if (senderId.equals(receiverId))
            throw new BusinessException(400, "Sender and receiver cannot be the same");
        if (userService.getById(receiverId) == null)
            throw new BusinessException(404, "User not found");
        if (ADMIN_ONLY.contains(type))
            receiverId = ADMIN_UNHANDLED;
        Request request = Request.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(type)
                .content(content)
                .access(Access.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        save(request);
        notificationService.notifyReq(
                senderId, receiverId,
                "New request: " + type.name(),
                NotificationType.REQUEST,
                request.getId(),
                ADMIN_ONLY.contains(type)
        );
        log.info("Request created: id={}, sender={}, type={}", request.getId(), senderId, type);
        return toVO(request, userService.getById(senderId));
    }

    @Override
    public PageVO<RequestVO> list(Long userId, int page, int size, boolean isAdmin) {
        Page<Request> result = lambdaQuery()
                .and(w -> {
                    w.eq(Request::getReceiverId, userId);
                    if (isAdmin) w.or().in(Request::getType, ADMIN_ONLY);
                })
                .orderByDesc(Request::getCreatedAt)
                .page(Page.of(page, size));
        List<RequestVO> vos = new ArrayList<>();
        for (Request r : result.getRecords())
            vos.add(toVO(r, userService.getById(r.getSenderId())));
        return PageVO.of(result, vos);
    }

    @Override
    @Transactional
    public RequestVO handle(Long userId, Long requestId, Access access, boolean isAdmin) {
        if (access == Access.PENDING)
            throw new BusinessException(400, "Invalid handle status");
        Request request = getById(requestId);
        if (request == null)
            throw new BusinessException(404, "Request not found");
        if ((ADMIN_ONLY.contains(request.getType()) && !isAdmin) ||
                !request.getReceiverId().equals(userId) && request.getReceiverId() != ADMIN_UNHANDLED)
            throw new BusinessException(403, "No permission to handle this request");
        if (request.getAccess() != Access.PENDING)
            throw new BusinessException(400, "Request already handled");
        request.setAccess(access);
        if (ADMIN_ONLY.contains(request.getType()))
            request.setReceiverId(userId);
        request.setHandledAt(LocalDateTime.now());
        updateById(request);
        if (access == Access.APPROVED) apply(request);
        log.info("Request handled: id={}, by={}, access={}", requestId, userId, access);
        return toVO(request, userService.getById(request.getSenderId()));
    }

    // ──────── Private tool ────────────────────────────────

    private void apply(Request request) {
        if (request.getType() == RequestType.STORAGE_EXTENSION) {
            Long capacity = parseCapacity(request.getContent());
            if (capacity == null || capacity < 0)
                throw new BusinessException(400, "Invalid capacity in request");
            User user = userService.getById(request.getSenderId());
            if (user == null) throw new BusinessException(404, "User not found");
            user.setCapacity(capacity);
            userService.updateById(user);
            log.info("Storage capacity applied: user={}, capacity={}", user.getId(), capacity);
        }
    }

    private Long parseCapacity(String content) {
        if (content == null || content.isBlank()) return null;
        try {
            JsonNode node = MAPPER.readTree(content);
            JsonNode capacity = node.get("capacity");
            return capacity != null && capacity.isNumber() ? capacity.asLong() : null;
        } catch (Exception e) {
            log.warn("Failed to parse request content: {}", content, e);
            return null;
        }
    }

    private RequestVO toVO(Request request, User sender) {
        return RequestVO.builder()
                .id(request.getId())
                .senderId(request.getSenderId())
                .senderName(sender != null ? sender.getUsername() : null)
                .receiverId(request.getReceiverId())
                .type(request.getType())
                .content(request.getContent())
                .access(request.getAccess())
                .handledAt(request.getHandledAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
