package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.Error;
import com.zincoid.me.mapper.ConfigMapper;
import com.zincoid.me.model.po.Config;
import com.zincoid.me.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements ConfigService {

    @Override
    public String get(String key) {
        Config config = lambdaQuery().eq(Config::getConfigKey, key).one();
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public String get(String key, String fallback) {
        String value = get(key);
        return value == null ? fallback : value;
    }

    @Override
    public Map<String, String> map() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Config config : list()) map.put(config.getConfigKey(), config.getConfigValue());
        return map;
    }

    @Override
    @Transactional
    public void update(String key, String value) {
        Config config = lambdaQuery().eq(Config::getConfigKey, key).one();
        if (config == null) {
            log.warn("Config key not found: {}", key);
            throw new BusinessException(Error.CONFIG_KEY_NOT_FOUND);
        }
        config.setConfigValue(value);
        updateById(config);
        log.info("Config updated: {}={}", key, value);
    }
}
