package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 *     Shop type entity
 * </p>
 *
 * @author Josephinesss
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_shop_type")
public class ShopType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * main id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * type name
     */
    private String name;

    /**
     * icons
     */
    private String icon;

    /**
     * sort
     */
    private Integer sort;

    /**
     * created time
     */
    @JsonIgnore
    private LocalDateTime createTime;

    /**
     * updated time
     */
    @JsonIgnore
    private LocalDateTime updateTime;


}
