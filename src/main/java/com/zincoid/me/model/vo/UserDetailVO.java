package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.Gender;
import com.zincoid.me.model.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserDetailVO {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private Gender gender;
    private String title;
    private String bio;
    private String avatar;
    private List<String> skills;
    private String contacts;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
