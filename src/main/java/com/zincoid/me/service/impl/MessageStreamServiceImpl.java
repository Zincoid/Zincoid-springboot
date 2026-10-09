package com.zincoid.me.service.impl;

import com.zincoid.me.model.vo.MessageVO;
import com.zincoid.me.service.MessageStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MessageStreamServiceImpl implements MessageStreamService {

    private static final long HEARTBEAT_INTERVAL = 15_000L;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe() {
        String id = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(id, emitter);
        emitter.onCompletion(() -> remove(id));
        emitter.onTimeout(() -> remove(id));
        emitter.onError(e -> remove(id));
        log.info("Chat stream subscribed: id={}", id);
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(id);
        }
        return emitter;
    }

    @Override
    public void broadcast(MessageVO message) {
        if (emitters.isEmpty()) return;
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("message").data(message));
            } catch (IOException e) {
                remove(id);
            }
        });
    }

    @Override
    public void delete(Long messageId) {
        if (emitters.isEmpty()) return;
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("delete").data(messageId));
            } catch (IOException e) {
                remove(id);
            }
        });
    }

    @Override
    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL)
    public void heartbeat() {
        if (emitters.isEmpty()) return;
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (IOException e) {
                remove(id);
            }
        });
    }

    // ──────── Private tool ────────────────────────────────

    private void remove(String id) {
        if (emitters.remove(id) == null) return;
        log.info("Chat stream unsubscribed: id={}", id);
    }
}
