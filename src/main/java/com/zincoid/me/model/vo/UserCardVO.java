package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.Gender;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserCardVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String title;
    private Gender gender;
    private Role role;
    private Status status;
    private LocalDateTime createdAt;
}
