package com.zincoid.me.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.me.model.enums.Approval;
import com.zincoid.me.model.enums.RepoAccessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("repo_access")
public class RepoAccess {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long repoId;
    private Long userId;
    private RepoAccessType type;
    private Approval approval;
    private LocalDateTime createdAt;
    @TableField(update = "NOW()")
    private LocalDateTime updatedAt;
}
