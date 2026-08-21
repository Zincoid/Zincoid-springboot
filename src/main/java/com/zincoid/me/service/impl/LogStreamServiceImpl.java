package com.zincoid.me.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.zincoid.me.model.vo.LogEntryVO;
import com.zincoid.me.service.LogStreamService;
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
public class LogStreamServiceImpl implements LogStreamService {

    private static final long HEARTBEAT_INTERVAL = 15_000L;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Level> filters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Level minLevel) {
        String id = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(id, emitter);
        filters.put(id, minLevel == null ? Level.INFO : minLevel);
        emitter.onCompletion(() -> remove(id));
        emitter.onTimeout(() -> remove(id));
        emitter.onError(e -> remove(id));
        log.info("Log stream subscribed: id={}, minLevel={}", id, minLevel);
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(id);
        }
        return emitter;
    }

    @Override
    public void broadcast(ILoggingEvent event) {
        if (emitters.isEmpty()) return;
        LogEntryVO entry = LogEntryVO.of(event);
        emitters.forEach((id, emitter) -> {
            Level min = filters.getOrDefault(id, Level.INFO);
            if (!event.getLevel().isGreaterOrEqual(min)) return;
            try {
                emitter.send(SseEmitter.event().data(entry));
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
        filters.remove(id);
        log.info("Log stream unsubscribed: id={}", id);
    }
}
