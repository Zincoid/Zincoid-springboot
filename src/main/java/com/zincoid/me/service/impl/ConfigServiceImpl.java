package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.ConfigMapper;
import com.zincoid.me.model.po.Config;
import com.zincoid.me.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements ConfigService {

    @Override
    public String get(String key) {
        Config config = lambdaQuery().eq(Config::getConfigKey, key).one();
        return config != null ? config.getConfigValue() : null;
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
        if (config == null) throw new BusinessException(404, "Config key not found: " + key);
        config.setConfigValue(value);
        updateById(config);
    }
}
