package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.po.Config;

import java.util.Map;

public interface ConfigService extends IService<Config> {

    String get(String key);

    Map<String, String> map();

    void update(String key, String value);
}
