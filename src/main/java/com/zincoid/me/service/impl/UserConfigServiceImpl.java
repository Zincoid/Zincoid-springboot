package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.converter.UserConfigConverter;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.UserConfigMapper;
import com.zincoid.me.model.dto.UserConfigUpdateRequest;
import com.zincoid.me.model.po.UserConfig;
import com.zincoid.me.model.vo.UserConfigVO;
import com.zincoid.me.service.UserConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserConfigServiceImpl extends ServiceImpl<UserConfigMapper, UserConfig> implements UserConfigService {

    @Override
    public UserConfig create(Long userId) {
        if (lambdaQuery().eq(UserConfig::getUserId, userId).exists())
            throw new BusinessException("User config already exists");
        UserConfig config = UserConfig.builder()
                .userId(userId)
                .receiveEmail(false)
                .build();
        save(config);
        return config;
    }

    @Override
    public UserConfigVO get(Long userId) {
        UserConfig config = lambdaQuery().eq(UserConfig::getUserId, userId).one();
        if (config == null) config = create(userId);
        return UserConfigConverter.INSTANCE.toVO(config);
    }

    @Override
    @Transactional
    public UserConfigVO update(Long userId, UserConfigUpdateRequest request) {
        UserConfig config = lambdaQuery().eq(UserConfig::getUserId, userId).one();
        if (config == null) config = create(userId);
        config.setReceiveEmail(request.getReceiveEmail());
        saveOrUpdate(config);
        log.info("User config updated: user={}, receiveEmail={}", userId, config.getReceiveEmail());
        return UserConfigConverter.INSTANCE.toVO(config);
    }

    @Override
    public void delete(Long userId) {
        lambdaUpdate().eq(UserConfig::getUserId, userId).remove();
    }
}
