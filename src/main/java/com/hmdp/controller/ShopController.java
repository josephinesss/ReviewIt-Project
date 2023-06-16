package com.hmdp.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * front-end controller
 * </p>
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * Search shop info by id
     * @param id shop id
     * @return shop data
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryById(id);
    }

    /**
     * newly created shop info
     * @param shop shop data
     * @return shop id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // write into db
        shopService.save(shop);
        // return shop id
        return Result.ok(shop.getId());
    }

    /**
     * update shop info
     * @param shop shop data
     * @return null
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // write into database
        return shopService.update(shop);
    }

    /**
     * paging and searching shop info by different shop types
     * @param typeId shop type
     * @param current page
     * @return shop list
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("x") Double x,
            @RequestParam("y") Double y
    ) {
        return shopService.queryShopByType(typeId, current, x, y);
    }

    /**
     * paging and searching shop info by keywords
     * @param name shop keywords
     * @param current page
     * @return shop list
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // paging search by type
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // return data
        return Result.ok(page.getRecords());
    }
}
