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
 *     Shops entity
 * </p>
 *
 * @author Josephinesss
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_shop")
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * main id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * shop name
     */
    private String name;

    /**
     * shop type id
     */
    private Long typeId;

    /**
     * shop images
     */
    private String images;

    /**
     * shop location area
     */
    private String area;

    /**
     * address
     */
    private String address;

    /**
     * longitude
     */
    private Double x;

    /**
     * latitude
     */
    private Double y;

    /**
     * average price as long
     */
    private Long avgPrice;

    /**
     * sales
     */
    private Integer sold;

    /**
     * number of comments
     */
    private Integer comments;

    /**
     * rating
     */
    private Integer score;

    /**
     * business hours
     */
    private String openHours;

    /**
     * created time
     */
    private LocalDateTime createTime;

    /**
     * updated time
     */
    private LocalDateTime updateTime;


    @TableField(exist = false)
    private Double distance;
}
