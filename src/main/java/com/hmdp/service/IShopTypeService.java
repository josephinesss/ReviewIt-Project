package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  Service
 * </p>
 *
 * @author Josephinesss
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryShopList();
}
