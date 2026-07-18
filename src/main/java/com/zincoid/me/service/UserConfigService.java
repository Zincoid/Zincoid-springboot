package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.dto.UserConfigUpdateRequest;
import com.zincoid.me.model.po.UserConfig;
import com.zincoid.me.model.vo.UserConfigVO;

public interface UserConfigService extends IService<UserConfig> {

    UserConfig create(Long userId);

    UserConfigVO get(Long userId);

    UserConfigVO update(Long userId, UserConfigUpdateRequest request);

    void delete(Long userId);
}
