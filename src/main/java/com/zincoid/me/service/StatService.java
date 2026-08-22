package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.po.Stat;

import java.util.Map;

public interface StatService extends IService<Stat> {

    void record(String method, String path);

    Map<String, Object> stats(int days, int top);
}
