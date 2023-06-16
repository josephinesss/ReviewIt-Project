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
 *     Voucher
 * </p>
 *
 * @author Josephinesss
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher")
public class Voucher implements Serializable {

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
     * coupon title
     */
    private String title;

    /**
     * coupon subtitle
     */
    private String subTitle;

    /**
     * rules for use
     */
    private String rules;

    /**
     * payment amount
     */
    private Long payValue;

    /**
     * actual payment amount
     */
    private Long actualValue;

    /**
     * coupon type
     */
    private Integer type;

    /**
     * coupon status
     */
    private Integer status;
    /**
     * stock
     */
    @TableField(exist = false)
    private Integer stock;

    /**
     * effective time
     */
    @TableField(exist = false)
    private LocalDateTime beginTime;

    /**
     * expiration time
     */
    @TableField(exist = false)
    private LocalDateTime endTime;

    /**
     * created time
     */
    private LocalDateTime createTime;


    /**
     * updated time
     */
    private LocalDateTime updateTime;


}
