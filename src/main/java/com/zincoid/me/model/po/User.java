package com.zincoid.me.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.me.model.enums.Gender;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private Gender gender;
    private String title;
    private String bio;
    private String avatar;
    private String skills;
    private String contacts;
    private Role role;
    private Status status;
    private LocalDateTime createdAt;
    @TableField(update = "NOW()")
    private LocalDateTime updatedAt;
}
