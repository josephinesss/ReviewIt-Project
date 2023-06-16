package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 *     User info entity
 * </p>
 *
 * @author Josephinesss
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user_info")
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * main id/ user id
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * city name
     */
    private String city;

    /**
     * personal introduction limited to 128 chars
     */
    private String introduce;

    /**
     * number of followers
     */
    private Integer fans;

    /**
     * number of followees
     */
    private Integer followee;

    /**
     * gender
     */
    private Boolean gender;

    /**
     * birthday
     */
    private LocalDate birthday;

    /**
     * credits gained
     */
    private Integer credits;

    /**
     * membership level from 0-9
     */
    private Boolean level;

    /**
     * created time
     */
    private LocalDateTime createTime;

    /**
     * updated time
     */
    private LocalDateTime updateTime;


}
