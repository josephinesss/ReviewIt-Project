package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 *   Blog Entity
 * </p>
 *
 * @author Josephinesss
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_blog")
public class Blog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * main id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * shop id
     */
    private Long shopId;
    /**
     * user id
     */
    private Long userId;
    /**
     * user icons
     */
    @TableField(exist = false)
    private String icon;
    /**
     * username
     */
    @TableField(exist = false)
    private String name;
    /**
     * whether user get thumbed up
     */
    @TableField(exist = false)
    private Boolean isLike;

    /**
     * title
     */
    private String title;

    /**
     * images, maximun for 9
     */
    private String images;

    /**
     * description of visiting a shop
     */
    private String content;

    /**
     * number of thumbs
     */
    private Integer liked;

    /**
     * number of comments
     */
    private Integer comments;

    /**
     * created time
     */
    private LocalDateTime createTime;

    /**
     * updated time
     */
    private LocalDateTime updateTime;


}
