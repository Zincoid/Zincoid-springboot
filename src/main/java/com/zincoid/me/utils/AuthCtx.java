package com.zincoid.me.utils;

import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.po.User;

public class AuthCtx {

    private static final ThreadLocal<Boolean> AUTHED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Role> ROLE = new ThreadLocal<>();

    public static void init(User user) {
        AUTHED.set(true);
        USER_ID.set(user.getId());
        ROLE.set(user.getRole());
    }

    public static void clear() {
        AUTHED.remove();
        USER_ID.remove();
        ROLE.remove();
    }

    public static boolean isAuthed() {
        return AUTHED.get();
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Role getRole() {
        return ROLE.get();
    }

    public static void requireLogin() {
        if (!AUTHED.get())
            throw new BusinessException(401, "Login required");
    }

    public static void requireAdmin() {
        requireLogin();
        if (getRole() != Role.ADMIN)
            throw new BusinessException(403, "Admin permission required");
    }
}
