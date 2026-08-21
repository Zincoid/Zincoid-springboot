package com.zincoid.me.model.vo;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record LogEntryVO(String timestamp, String level, String logger, String thread,
                         String message, String stackTrace) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static LogEntryVO of(ILoggingEvent event) {
        String stack = event.getThrowableProxy() == null
                ? null : ThrowableProxyUtil.asString(event.getThrowableProxy());
        return new LogEntryVO(
                FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()).atZone(ZoneId.systemDefault())),
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getThreadName(),
                event.getFormattedMessage(),
                stack);
    }
}
