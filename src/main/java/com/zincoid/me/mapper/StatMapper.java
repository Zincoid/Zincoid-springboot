package com.zincoid.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.me.model.po.Stat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface StatMapper extends BaseMapper<Stat> {

    @Insert("INSERT INTO stat (`stat_date`, `api`, `count`) VALUES (#{date}, #{api}, #{count}) " +
            "ON DUPLICATE KEY UPDATE `count` = VALUES(`count`)")
    void upsert(@Param("date") LocalDate date, @Param("api") String api, @Param("count") long count);
}
