package com.zincoid.me.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("repo_item")
public class RepoItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long repoId;
    private Integer sortOrder;
    private Long fileId;
    private String name;
    private Status status;
    private LocalDateTime createdAt;
}
