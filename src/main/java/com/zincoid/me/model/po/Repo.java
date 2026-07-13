package com.zincoid.me.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.me.model.enums.RepoType;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.enums.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("repo")
public class Repo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private RepoType type;
    private String url;
    private String tags;
    private String coverImage;
    private Status status;
    private Visibility visibility;
    private Long viewCount;
    private LocalDateTime createdAt;
    @TableField(update = "NOW()")
    private LocalDateTime updatedAt;
}
