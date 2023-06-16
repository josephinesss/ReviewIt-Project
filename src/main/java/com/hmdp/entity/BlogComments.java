package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 *     Blog Comment Entity
 * </p>
 *
 * @author Josephinesss
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_blog_comments")
public class BlogComments implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * main id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * user id
     */
    private Long userId;

    /**
     * blog id
     */
    private Long blogId;

    /**
     * The associated first-level comment id.
     * If it is a first-level comment, the value is 0
     */
    private Long parentId;

    /**
     * reply comment id
     */
    private Long answerId;

    /**
     * reply content
     */
    private String content;

    /**
     * number of thumbs
     */
    private Integer liked;

    /**
     * status，0：normal，1：being reported，2：no permission to see
     */
    private Boolean status;

    /**
     * created time
     */
    private LocalDateTime createTime;

    /**
     * updated time
     */
    private LocalDateTime updateTime;


}
