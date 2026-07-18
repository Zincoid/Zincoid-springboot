package com.zincoid.me.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_config")
public class UserConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Boolean receiveEmail;
    private LocalDateTime createdAt;
    @TableField(update = "NOW()")
    private LocalDateTime updatedAt;
}
