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
 *     Voucher order entity
 * </p>
 *
 * @author Josephinesss
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher_order")
public class VoucherOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * main id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * user id
     */
    private Long userId;

    /**
     * coupon id
     */
    private Long voucherId;

    /**
     * payment type
     */
    private Integer payType;

    /**
     * order status
     */
    private Integer status;

    /**
     * order time
     */
    private LocalDateTime createTime;

    /**
     * payment time
     */
    private LocalDateTime payTime;

    /**
     * usage time
     */
    private LocalDateTime useTime;

    /**
     * refund time
     */
    private LocalDateTime refundTime;

    /**
     * update time
     */
    private LocalDateTime updateTime;


}
