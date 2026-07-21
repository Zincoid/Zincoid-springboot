package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserConfigVO {

    private Boolean receiveEmail;
    private Boolean receiveEmailSys;
    private Boolean receiveEmailRepoAccess;
}
