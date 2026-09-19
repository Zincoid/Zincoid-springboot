package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContributorVO {

    private Long userId;
    private String nickname;
    private String avatar;
}
