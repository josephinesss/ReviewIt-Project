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
 *     User entity
 * </p>
 *
 * @author Josephinesss
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * main id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * phone number
     */
    private String phone;

    /**
     * poassword
     */
    private String password;

    /**
     * username, random chars by default
     */
    private String nickName;

    /**
     * user icons
     */
    private String icon = "";

    /**
     * created time
     */
    private LocalDateTime createTime;

    /**
     * updated time
     */
    private LocalDateTime updateTime;


}
