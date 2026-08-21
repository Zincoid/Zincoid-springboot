package com.zincoid.me.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface LogStreamService {

    SseEmitter subscribe(Level minLevel);

    void broadcast(ILoggingEvent event);

    void heartbeat();
}
