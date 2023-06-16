package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  Service implementation
 * </p>
 *
 * @author Josephinesss
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopList() {
        List<String> shopCache = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);
        if(shopCache.size() != 0){
            ArrayList<ShopType> shopTypes = new ArrayList<>();
            for(String shopStr : shopCache){
                shopTypes.add(JSONUtil.toBean(shopStr, ShopType.class));
            }
            return Result.ok(shopTypes);
        }

        List<ShopType> typeList = query().orderByAsc("sort").list();

        if(typeList.isEmpty()){
            return Result.fail("Fail to get shop list");
        }

        for(ShopType s : typeList){
            String strJson = JSONUtil.toJsonStr(s);
            shopCache.add(strJson);
        }

        stringRedisTemplate.opsForList().leftPushAll(CACHE_SHOP_TYPE_KEY, shopCache);
        return Result.ok(typeList);
    }
}
