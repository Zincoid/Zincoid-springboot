-- =============================================
-- Zincoid Personal Website Database Schema
-- Version: 1.0
-- Engine: InnoDB, Charset: utf8mb4
-- =============================================

CREATE DATABASE IF NOT EXISTS zincoid DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zincoid;

-- =============================================
-- 1. User Account Table
-- =============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary Key',
    `username`      VARCHAR(50)     NOT NULL                 COMMENT 'Username (login)',
    `password`      VARCHAR(255)    NOT NULL                 COMMENT 'BCrypt hashed password',
    `nickname`      VARCHAR(50)     DEFAULT NULL             COMMENT 'Display nickname',
    `gender`        TINYINT         DEFAULT NULL             COMMENT 'Gender: 0=MALE(he/him), 1=FEMALE(she/her), null=hidden',
    `title`         VARCHAR(100)    DEFAULT NULL             COMMENT 'Identity title / headline',
    `bio`           TEXT            DEFAULT NULL             COMMENT 'Personal bio / introduction',
    `avatar`        VARCHAR(500)    DEFAULT NULL             COMMENT 'Avatar image URL',
    `skills`        JSON            DEFAULT NULL             COMMENT 'Skill tags array',
    `contacts`      JSON            DEFAULT NULL             COMMENT 'Multi-platform contacts (GitHub, Bilibili, Email, etc.)',
    `role`          TINYINT         NOT NULL DEFAULT 0       COMMENT 'Role: 0=USER, 1=ADMIN',
    `status`        TINYINT         NOT NULL DEFAULT 1       COMMENT 'Status: 0=DISABLED, 1=ACTIVE',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Registration time',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User account table';

-- =============================================
-- 2. Moment (Feed / 朋友圈) Table
-- =============================================
CREATE TABLE IF NOT EXISTS `moment` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary Key',
    `user_id`       BIGINT          NOT NULL                 COMMENT 'Author user ID',
    `content`       TEXT            DEFAULT NULL             COMMENT 'Text content',
    `images`        JSON            DEFAULT NULL             COMMENT 'Image URLs array',
    `is_pinned`     BOOLEAN         NOT NULL DEFAULT FALSE   COMMENT 'Pinned (admin only)',
    `view_count`    BIGINT          NOT NULL DEFAULT 0       COMMENT 'View count',
    `status`        TINYINT         NOT NULL DEFAULT 1       COMMENT 'Status: 0=DISABLED, 1=ACTIVE',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Post time',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_pinned` (`is_pinned`),
    KEY `idx_view_count` (`view_count`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_moment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Moment / Feed table';

-- =============================================
-- 3. Unified Comment Table (for both moments and articles)
-- =============================================
CREATE TABLE IF NOT EXISTS `comment` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary Key',
    `target_type`   TINYINT         NOT NULL                 COMMENT 'Target type: 0=MOMENT, 1=ARTICLE',
    `target_id`     BIGINT          NOT NULL                 COMMENT 'Target record ID',
    `user_id`       BIGINT          NOT NULL                 COMMENT 'Commenter user ID',
    `parent_id`     BIGINT          DEFAULT NULL             COMMENT 'Parent comment ID (NULL=top-level, non-null=reply)',
    `content`       TEXT            NOT NULL                 COMMENT 'Comment content',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Comment time',
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Unified comment table for moments and articles';

-- =============================================
-- 4. Article (Markdown Long-form) Table
-- =============================================
CREATE TABLE IF NOT EXISTS `article` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary Key',
    `user_id`       BIGINT          NOT NULL                 COMMENT 'Author user ID',
    `title`         VARCHAR(200)    NOT NULL                 COMMENT 'Article title',
    `content_md`    LONGTEXT        NOT NULL                 COMMENT 'Original markdown content',
    `content_html`  LONGTEXT        DEFAULT NULL             COMMENT 'Parsed HTML content',
    `summary`       VARCHAR(500)    DEFAULT NULL             COMMENT 'Article summary / excerpt',
    `cover_image`   VARCHAR(500)    DEFAULT NULL             COMMENT 'Cover image URL',
    `is_pinned`     BOOLEAN         NOT NULL DEFAULT FALSE   COMMENT 'Pinned (admin only)',
    `status`        TINYINT         NOT NULL DEFAULT 1       COMMENT 'Status: 0=draft, 1=published',
    `view_count`    BIGINT          NOT NULL DEFAULT 0       COMMENT 'View count',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Publish time',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_title` (`title`),
    KEY `idx_is_pinned` (`is_pinned`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_view_count` (`view_count`),
    CONSTRAINT `fk_article_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Markdown article table';


-- =============================================
-- 5. File Record Table
-- =============================================
CREATE TABLE IF NOT EXISTS `file` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary Key',
    `user_id`       BIGINT          NOT NULL                 COMMENT 'Uploader user ID',
    `file_name`     VARCHAR(255)    NOT NULL                 COMMENT 'Original file name',
    `file_path`     VARCHAR(500)    NOT NULL                 COMMENT 'Server-side file path',
    `file_type`     TINYINT         NOT NULL DEFAULT 0       COMMENT 'File type: 0=IMAGE, 1=VIDEO, 2=AUDIO, 3=OTHER',
    `file_size`     BIGINT          NOT NULL DEFAULT 0       COMMENT 'File size in bytes',
    `related_type`  TINYINT         DEFAULT NULL             COMMENT 'Related business type: 0=MOMENT, 1=ARTICLE, 2=AVATAR',
    `related_id`    BIGINT          DEFAULT NULL             COMMENT 'Related business record ID',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload time',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_related` (`related_type`, `related_id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_file_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File record table';

-- =============================================
-- 6. Like Table (for both moments and articles)
-- =============================================
CREATE TABLE IF NOT EXISTS `likes` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary Key',
    `target_type`   TINYINT         NOT NULL                 COMMENT 'Target type: 0=MOMENT, 1=ARTICLE',
    `target_id`     BIGINT          NOT NULL                 COMMENT 'Target record ID',
    `user_id`       BIGINT          NOT NULL                 COMMENT 'User who liked',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Like time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_target_user` (`target_type`, `target_id`, `user_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_likes_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Unified likes table for moments and articles';

-- =============================================
-- 7. Configuration Table
-- =============================================
CREATE TABLE IF NOT EXISTS `config` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary Key',
    `config_key`    VARCHAR(100)    NOT NULL                 COMMENT 'Configuration key',
    `config_value`  TEXT            DEFAULT NULL             COMMENT 'Configuration value',
    `description`   VARCHAR(255)    DEFAULT NULL             COMMENT 'Description of the config item',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Configuration table';

-- =============================================
-- 8. Notification Table
-- =============================================
CREATE TABLE IF NOT EXISTS `notification` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT  COMMENT 'Primary Key',
    `sender_id`     BIGINT      NOT NULL                 COMMENT 'User who triggered the notification',
    `receiver_id`   BIGINT      NOT NULL                 COMMENT 'User who receives the notification',
    `related_type`  TINYINT     NOT NULL                 COMMENT 'Related target type: 0=MOMENT, 1=ARTICLE',
    `related_id`    BIGINT      NOT NULL                 COMMENT 'Related moment/article ID',
    `comment_id`    BIGINT      NOT NULL                 COMMENT 'The comment that triggered this notification',
    `is_read`       BOOLEAN     NOT NULL DEFAULT FALSE   COMMENT 'Whether the notification has been read',
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Notification time',
    PRIMARY KEY (`id`),
    KEY `idx_receiver_read` (`receiver_id`, `is_read`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_notification_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notification_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Notification table';

-- =============================================
-- Default admin user is auto-created by DataInitializer on first startup
-- =============================================

-- =============================================
-- Default site configurations
-- =============================================
INSERT INTO `config` (`config_key`, `config_value`, `description`) VALUES
('site_name', 'Zincoid', 'Website name'),
('site_description', 'Personal website and blog', 'Website description'),
('site_keywords', 'blog,tech,personal', 'SEO keywords'),
('page_size', '10', 'Default pagination page size')
ON DUPLICATE KEY UPDATE `config_key` = VALUES(`config_key`);
