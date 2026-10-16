package com.zincoid.me.model;

import lombok.Getter;

/**
 * Stable error identifiers returned as ApiResp.error.
 * The frontend translates via en.js/zh.js keys matching the enum name;
 * {@code message} is the English fallback when no translation exists.
 */
@Getter
public enum Error {

    // ── Auth / session ────────────────────────────────────
    LOGIN_REQUIRED(401, "Login required"),
    TOKEN_INVALID(401, "Token is invalid"),
    TOKEN_REVOKED(401, "Token is revoked"),
    ADMIN_REQUIRED(403, "Admin required"),
    ACCOUNT_NOT_FOUND(404, "Account not found"),
    ACCOUNT_DISABLED(403, "Account is disabled"),

    // ── User ──────────────────────────────────────────────
    USER_NOT_FOUND(404, "User not found"),
    USER_DISABLED(403, "User is disabled"),
    CANNOT_DISABLE_ADMIN(403, "Cannot disable an admin account"),
    INVALID_CREDENTIALS(401, "Invalid username or password"),
    USERNAME_ALREADY_EXISTS(409, "Username already exists"),
    USERNAME_ALREADY_TAKEN(409, "Username already taken"),
    USERNAME_CANNOT_BE_EMPTY(400, "Username cannot be empty"),
    OLD_PASSWORD_INCORRECT(400, "Old password is incorrect"),
    USER_CONFIG_ALREADY_EXISTS(409, "User config already exists"),

    // ── Email / verification code ─────────────────────────
    EMAIL_REQUIRED(400, "Email is required"),
    EMAIL_ALREADY_REGISTERED(409, "Email already registered"),
    EMAIL_NOT_REGISTERED(404, "Email not registered"),
    NO_EMAIL_SET(400, "No email set"),
    NEW_EMAIL_SAME(400, "New email is the same as current email"),
    VERIFICATION_CODE_INVALID(400, "Invalid or expired verification code"),
    OLD_EMAIL_CODE_INVALID(400, "Invalid or expired old email verification code"),
    NEW_EMAIL_CODE_INVALID(400, "Invalid or expired new email verification code"),
    VERIFICATION_CODE_RESEND_LIMIT(429, "Verification code already requested, please wait"),

    // ── Content targets ───────────────────────────────────
    MOMENT_NOT_FOUND(404, "Moment not found"),
    MOMENT_IS_PRIVATE(403, "Moment is private"),
    ARTICLE_NOT_FOUND(404, "Article not found"),
    ARTICLE_IS_PRIVATE(403, "Article is private"),
    REPO_NOT_FOUND(404, "Repo not found"),
    REPO_IS_PRIVATE(403, "Repo is private"),
    REPO_IS_RESTRICTED(403, "Repo is restricted"),
    REPO_NOT_RESTRICTED(403, "Repo is not restricted"),
    REPO_IS_CODE_TYPE(403, "Repo is code type"),
    REPO_RESTRICTED_NEED_VIEWER(403, "Repo is restricted and you need viewer access first"),
    REPO_NOT_TRANSFERABLE(400, "Repo is no longer transferable"),
    COMMENT_NOT_FOUND(404, "Comment not found"),
    MESSAGE_NOT_FOUND(404, "Message not found"),
    NOTIFICATION_NOT_FOUND(404, "Notification not found"),
    ITEM_NOT_FOUND(404, "Item not found"),
    INVALID_ITEM_SORT(400, "Invalid item sort"),
    REQUEST_NOT_FOUND(404, "Request not found"),
    REQUEST_ALREADY_HANDLED(400, "Request already handled"),
    ACCESS_RECORD_NOT_FOUND(404, "Access record not found"),

    // ── Ownership / permission on content ─────────────────
    ONLY_EDIT_OWN_MOMENTS(403, "You can only edit your own moments"),
    ONLY_EDIT_OWN_ARTICLES(403, "You can only edit your own articles"),
    ONLY_EDIT_OWN_REPOS(403, "You can only edit your own repos"),
    ONLY_DELETE_OWN_ITEMS(403, "You can only delete your own items"),
    ONLY_DELETE_OWN_MESSAGES(403, "You can only delete your own messages"),
    NO_PERMISSION_DELETE_MOMENT(403, "No permission to delete this moment"),
    NO_PERMISSION_DELETE_ARTICLE(403, "No permission to delete this article"),
    NO_PERMISSION_DELETE_REPO(403, "No permission to delete this repo"),
    NO_PERMISSION_DELETE_COMMENT(403, "No permission to delete this comment"),
    NO_PERMISSION_DELETE_MUSIC(403, "No permission to delete this music"),
    NO_PERMISSION_DELETE_NOTIFICATION(403, "No permission to delete this notification"),
    NO_PERMISSION_DELETE_REQUEST(403, "No permission to delete this request"),
    NO_PERMISSION_HANDLE_REQUEST(403, "No permission to handle this request"),
    NO_PERMISSION_UPLOAD_MUSIC(403, "No permission to upload public music"),
    NO_PERMISSION_ADD_ITEM(403, "No permission to add item"),
    NO_PERMISSION_DELETE_ITEM(403, "No permission to delete item"),
    NO_PERMISSION_SWAP_ITEM(403, "No permission to swap item"),
    NO_PERMISSION_PIN_COMMENT(403, "No permission to pin this comment"),
    NO_PERMISSION_UNPIN_COMMENT(403, "No permission to unpin this comment"),
    PIN_ROOT_COMMENT_ONLY(400, "Only top-level comments can be pinned"),
    ONLY_OWNER_MANAGE_ACCESS(403, "Only repo owner can manage access"),
    YOU_ARE_THE_OWNER(400, "You are the owner"),
    NOT_A_CONTRIBUTOR(404, "You are not a contributor"),

    // ── Repo access ───────────────────────────────────────
    ACCESS_ALREADY_REQUESTED(400, "Access already requested"),
    ACCESS_ALREADY_APPROVED(400, "Access already approved"),
    ACCESS_ALREADY_REJECTED(400, "Access already rejected"),
    ACCESS_ALREADY_RESOLVED(400, "Access already resolved"),

    // ── Request flow ──────────────────────────────────────
    REQUEST_TYPE_INVALID(400, "Request type is invalid"),
    INVALID_REQUEST_META(400, "Invalid request meta"),
    INVALID_HANDLE_STATUS(400, "Invalid handle status"),
    SENDER_RECEIVER_SAME(400, "Sender and receiver cannot be the same"),

    // ── File / storage ────────────────────────────────────
    PATH_INVALID(404, "Path is invalid"),
    FILE_NOT_FOUND(404, "File not found"),
    PERMISSION_DENIED(403, "Permission denied"),
    FILE_EMPTY(400, "File is empty"),
    FILE_TYPE_FORBIDDEN(400, "Forbidden file type"),
    FILE_TYPE_UNSUPPORTED(400, "Unsupported file type"),
    FILE_SAVE_FAILED(500, "File save failed"),
    STORAGE_SPACE_FAILED(500, "Failed to get storage space"),
    INSUFFICIENT_PERSONAL_STORAGE(400, "Insufficient personal storage space"),
    INSUFFICIENT_SERVER_DISK(500, "Insufficient server disk space"),
    CAPACITY_INVALID(400, "Capacity must be a non-negative number"),

    // ── Music ─────────────────────────────────────────────
    NOT_A_MUSIC_FILE(400, "Not a music file"),

    // ── Config ────────────────────────────────────────────
    CONFIG_KEY_NOT_FOUND(404, "Config key not found"),

    // ── Permission service ────────────────────────────────
    PERMISSION_REQUIRED(403, "Permission required"),
    PERMISSION_NOT_FOUND(404, "Permission not found"),
    PERMISSION_ALREADY_GRANTED(400, "Permission already granted"),

    // ── Request parameter checks ──────────────────────────
    CONTENT_REQUIRED(400, "Content is required"),
    CONTENT_OR_FILE_REQUIRED(400, "Content or file is required"),
    AVATAR_REQUIRED(400, "Avatar is required"),
    USERNAME_PASSWORD_REQUIRED(400, "Username and password are required"),
    EMAIL_NEW_CODE_REQUIRED(400, "Email and newCode are required"),
    INVALID_DAYS_RANGE(400, "days must be between 1 and 365"),
    INVALID_TOP_RANGE(400, "top must be between 0 and 500"),
    INVALID_PARAMETER(400, "Invalid parameter"),

    // ── AI ────────────────────────────────────────────────
    AI_USER_NOT_INITIALIZED(500, "AI user not initialized"),
    AI_PROVIDER_NOT_CONFIGURED(500, "AI provider not configured (ai.openai.providers)"),
    AI_INSUFFICIENT_BALANCE(502, "Model API has insufficient balance"),
    AI_EMPTY_RESPONSE(502, "Empty response from AI model API"),
    AI_NO_MESSAGE(502, "No available message in AI response"),
    AI_REASONING_TRUNCATED(502, "Request too complex, reasoning was interrupted"),

    // ── System ────────────────────────────────────────────
    INTERNAL_SERVER_ERROR(500, "Internal server error"),
    SERVER_UNDER_MAINTENANCE(503, "Server is under maintenance");

    private final int code;
    private final String message;

    Error(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
