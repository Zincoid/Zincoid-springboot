package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.dto.LoginRequest;
import com.zincoid.me.model.dto.RegisterRequest;
import com.zincoid.me.model.dto.UserUpdateRequest;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.LoginVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.UserCardVO;
import com.zincoid.me.model.vo.UserDetailVO;

public interface UserService extends IService<User> {

    LoginVO register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    void logout(String token);

    PageVO<UserCardVO> list(int page, int size, Role role, boolean isActive);

    UserDetailVO get(Long userId);

    UserDetailVO update(Long userId, UserUpdateRequest request);

    void delete(Long userId);

    UserDetailVO updateAvatar(Long userId, String avatar);

    void changePassword(Long userId, String oldPassword, String newPassword);

    void resetPassword(String username, String newPassword);

    void updateStatus(Long userId, Status status);

    boolean isTokenRevoked(String token);
}
