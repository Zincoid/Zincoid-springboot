package com.zincoid.me.utils;

import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.enums.Role;

public class AuthCtx {

    private static final ThreadLocal<Boolean> AUTHED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Role> ROLE = new ThreadLocal<>();

    public static void set(Long userId, String username, Role role) {
        AUTHED.set(true);
        USER_ID.set(userId);
        USERNAME.set(username);
        ROLE.set(role);
    }

    public static boolean isAuthed() {
        return AUTHED.get();
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static Role getRole() {
        return ROLE.get();
    }

    public static void clear() {
        AUTHED.remove();
        USER_ID.remove();
        USERNAME.remove();
        ROLE.remove();
    }

    public static void requireLogin() {
        if (getUserId() == null)
            throw new BusinessException(401, "Login required");
    }

    public static void requireAdmin() {
        if (getRole() != Role.ADMIN)
            throw new BusinessException(403, "Admin permission required");
    }
}
