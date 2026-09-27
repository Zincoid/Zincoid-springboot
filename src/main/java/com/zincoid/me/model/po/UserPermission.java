package com.zincoid.me.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.me.model.enums.Perm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_permission")
public class UserPermission {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Perm perm;
    private Long grantedBy;
    private LocalDateTime createdAt;
}
