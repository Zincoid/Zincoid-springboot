package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.MessageMapper;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.po.Message;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.MessageVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.ConfigService;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.MessageService;
import com.zincoid.me.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final UserService userService;
    private final FileService fileService;
    private final ConfigService configService;

    public MessageServiceImpl(UserService userService, FileService fileService, ConfigService configService) {
        this.userService = userService;
        this.fileService = fileService;
        this.configService = configService;
    }

    @Override
    @Transactional
    public MessageVO send(Long userId, String content, String file) {
        if ((content == null || content.isBlank()) && (file == null || file.isBlank()))
            throw new BusinessException(400, "Content or file is required");
        Message msg = Message.builder()
                .userId(userId)
                .content(content != null && !content.isBlank() ? content : null)
                .file(file != null && !file.isBlank() ? file : null)
                .createdAt(LocalDateTime.now())  // 无法回填需手动设置
                .build();
        save(msg);
        if (msg.getFile() != null)
            fileService.link(List.of(msg.getFile()), RelatedType.CHAT, msg.getId());
        trim();
        log.info("Message sent: user={}, id={}", userId, msg.getId());
        return buildVO(msg);
    }

    @Override
    public PageVO<MessageVO> list(int page, int size) {
        Page<Message> msgPage = lambdaQuery()
                .orderByAsc(Message::getCreatedAt)
                .page(Page.of(page, size));
        return PageVO.of(msgPage, this::buildVO);
    }

    // ──────── Private tool ────────────────────────────────

    private void trim() {
        String maxStr = configService.get("message_max_count");
        int max = 200;
        try { if (maxStr != null) max = Integer.parseInt(maxStr); }
        catch (NumberFormatException ignored) {}
        long total = count();
        if (total <= max) return;
        int toDelete = (int) (total - max);
        List<Message> oldest = lambdaQuery()
                .orderByAsc(Message::getCreatedAt)
                .last("LIMIT " + toDelete)
                .list();
        for (Message m : oldest) {
            if (m.getFile() != null)
                fileService.delete(RelatedType.CHAT, m.getId());
            removeById(m.getId());
        }
    }

    private MessageVO buildVO(Message msg) {
        User user = userService.getById(msg.getUserId());
        return MessageVO.builder()
                .id(msg.getId())
                .userId(msg.getUserId())
                .userNickname(user != null ? user.getNickname() : null)
                .userAvatar(user != null ? user.getAvatar() : null)
                .content(msg.getContent())
                .file(msg.getFile())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
