package com.zincoid.me.model.dto;

import com.zincoid.me.model.enums.Gender;
import com.zincoid.me.validation.ValidJsonObject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    private String nickname;
    private Gender gender;
    private String title;
    private String bio;
    private List<String> skills;

    @ValidJsonObject(message = "Contacts must be a valid JSON object")
    private String contacts;
}
