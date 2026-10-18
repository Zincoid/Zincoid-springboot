package com.zincoid.me.model;

import lombok.Getter;

/**
 * Site config keys seeded by DataInitializer and read via ConfigService.
 * The database value wins; {@code defaultValue} is the fallback when the row is missing.
 */
@Getter
public enum Conf {

    SITE_NAME("site_name", "Zincoid's", "Website name"),
    SITE_DESC_EN("site_desc_en", "Personal website and blogs", "Website description (English)"),
    SITE_DESC_ZH("site_desc_zh", "个人网站与博客", "Website description (Chinese)"),
    PAGE_SIZE("page_size", "10", "Default pagination page size"),
    MESSAGE_MAX_COUNT("message_max_count", "100", "Maximum number of messages to keep"),
    LOADING_SPINNER_HOLD("loading_spinner_hold", "250", "Loading spinner hold duration (ms) before fade"),
    LOADING_SPINNER_FADE("loading_spinner_fade", "125", "Loading spinner fade-out duration (ms)"),
    HERO_ANIMATION("hero_animation", "random", "Hero animation: squares, raindrop, raindrop_sin, or random"),
    AUDIO_SPECTRUM_RATIO("audio_spectrum_ratio", "0.2", "Audio spectrum ratio of digital flow for Walkman"),
    MAINTENANCE_ENABLED("maintenance_enabled", "true", "Enable daily maintenance (UTC+8 00:00-00:10, auto cleanup and block all requests)"),
    AI_CHAT_PROMPT("ai_chat_prompt", "你是 Zincoid 网站聊天室里的 AI 助手。请自然地融入对话，回复简洁友好，不要长篇大论。", "AI chat system prompt"),
    AI_COMMENT_PROMPT("ai_comment_prompt", "你是 Zincoid 网站的 AI 助手。用户在评论区@了你，回答将回复到那条评论下，请结合上下文回复。了解资源内容可自行调用工具。", "AI comment system prompt");

    private final String key;
    private final String defaultValue;
    private final String description;

    Conf(String key, String defaultValue, String description) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
    }
}
