package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.UserMapper;
import com.zincoid.me.converter.UserConverter;
import com.zincoid.me.model.dto.LoginRequest;
import com.zincoid.me.model.dto.RegisterRequest;
import com.zincoid.me.model.dto.UserUpdateRequest;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.po.Article;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.vo.LoginVO;
import com.zincoid.me.model.vo.UserCardVO;
import com.zincoid.me.model.vo.UserDetailVO;
import com.zincoid.me.service.ArticleService;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.service.UserService;

import com.zincoid.me.utils.JsonUtil;
import com.zincoid.me.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTool jwtTool;

    private final MomentService momentService;
    private final ArticleService articleService;
    private final FileService fileService;

    @Override
    @Transactional
    public LoginVO register(RegisterRequest request) {
        if (lambdaQuery().eq(User::getUsername, request.getUsername()).exists())
            throw new BusinessException("Username already exists");
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null && !request.getNickname().isBlank()
                        ? request.getNickname() : request.getUsername())
                .role(Role.USER)  // 无法回填需手动设置
                .build();
        save(user);
        log.info("User registered: {}", user.getUsername());
        String token = jwtTool.generate(user.getId(), user.getUsername(), user.getRole());
        return LoginVO.builder()
                .token(token)
                .user(UserConverter.INSTANCE.toDetailVO(user))
                .build();
    }

    @Override
    public LoginVO login(LoginRequest request) {
        User user = lambdaQuery().eq(User::getUsername, request.getUsername()).one();
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new BusinessException("Invalid username or password");
        if (user.getStatus() == Status.DISABLED)
            throw new BusinessException("Account is disabled");
        String token = jwtTool.generate(user.getId(), user.getUsername(), user.getRole());
        log.info("User logged in: {}", user.getUsername());
        return LoginVO.builder()
                .token(token)
                .user(UserConverter.INSTANCE.toDetailVO(user))
                .build();
    }

    @Override
    public void logout(String token) {
        if (token == null) return;
        try {
            long expiration = jwtTool.parse(token).getExpiration().getTime();
            revokedTokens.put(token, expiration);
        } catch (Exception ignored) {}
    }

    @Override
    public PageVO<UserCardVO> list(int page, int size, Role role) {
        Page<User> userPage = lambdaQuery()
                .eq(User::getStatus, Status.ACTIVE)
                .eq(role != null, User::getRole, role)
                .orderByAsc(User::getCreatedAt)
                .page(Page.of(page, size));
        return PageVO.of(userPage, UserConverter.INSTANCE::toCardVO);
    }

    @Override
    public UserDetailVO get(Long userId) {
        User user = getById(userId);
        if (user.getStatus() == Status.DISABLED)
            throw new BusinessException(403, "User is disabled");
        return UserConverter.INSTANCE.toDetailVO(user);
    }

    @Override
    @Transactional
    public UserDetailVO update(Long userId, UserUpdateRequest request) {
        User user = getById(userId);
        if (request.getUsername() != null) {
            String newUsername = request.getUsername().trim();
            if (newUsername.isEmpty())
                throw new BusinessException(400, "Username cannot be empty");
            if (!newUsername.equals(user.getUsername())) {
                User existing = lambdaQuery().eq(User::getUsername, newUsername).one();
                if (existing != null && !existing.getId().equals(userId))
                    throw new BusinessException(409, "Username already taken");
                user.setUsername(newUsername);
            }
        }
        if (request.getNickname() != null) {
            String nickname = request.getNickname().trim();
            user.setNickname(nickname.isEmpty() ? user.getUsername() : nickname);
        }
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getTitle() != null) user.setTitle(request.getTitle());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getSkills() != null) {
            String json = JsonUtil.toJson(request.getSkills());
            user.setSkills(json);
        }
        if (request.getContacts() != null) {
            String contacts = request.getContacts().trim();
            user.setContacts(contacts.isEmpty() ? null : contacts);
        }
        updateById(user);
        return UserConverter.INSTANCE.toDetailVO(user);
    }

    @Override
    @Transactional
    public UserDetailVO updateAvatar(Long userId, String avatar) {
        User user = getById(userId);
        if (user.getAvatar() != null && !user.getAvatar().equals(avatar))
            fileService.delete(user.getAvatar());
        user.setAvatar(avatar);
        updateById(user);
        return UserConverter.INSTANCE.toDetailVO(user);
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        User user = getById(userId);
        List<Moment> moments = momentService.lambdaQuery().eq(Moment::getUserId, userId).list();
        for (Moment m : moments) momentService.delete(userId, m.getId(), false);
        List<Article> articles = articleService.lambdaQuery().eq(Article::getUserId, userId).list();
        for (Article a : articles) articleService.delete(userId, a.getId(), false);
        if (user.getAvatar() != null) fileService.delete(user.getAvatar());
        removeById(userId);
        log.info("User account deleted: {}", userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword()))
            throw new BusinessException("Old password is incorrect");
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    @Override
    public boolean isTokenRevoked(String token) {
        revokedTokens.entrySet().removeIf(e -> e.getValue() < System.currentTimeMillis());
        return revokedTokens.containsKey(token);
    }

    // ──────── Private tool ────────────────────────────────

    private User getById(Long userId) {
        User user = super.getById(userId);
        if (user == null) throw new BusinessException(404, "User not found");
        return user;
    }
}
